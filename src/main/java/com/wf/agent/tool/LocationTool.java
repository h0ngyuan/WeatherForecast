package com.wf.agent.tool;

import cn.dev33.satoken.context.SaHolder;
import com.wf.mapper.CityInfoMapper;
import com.wf.object.entity.CityInfoEntity;
import com.wf.utils.LocationUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LocationTool {

    @Autowired
    private CityInfoMapper cityInfoMapper;

    /**
     * 获取离当前位置最近的可预测城市
     * @return 最近城市的名称
     */
    @Tool(description = "获取离当前位置最近的可预测城市（返回JSON包含城市名和经纬度）。如果该城市不在数据库中，会自动异步添加到数据库")
    public String getNearestAvailableCity() throws UnknownHostException {
        Map<String, Object> currentLocation = LocationUtils.getCurrentLocationMap();
        if (currentLocation == null) {
            return buildResultJson("成都", 30.5728, 104.0668);
        }

        Double lat = (Double) currentLocation.get("lat");
        Double lon = (Double) currentLocation.get("lon");
        String cityName = currentLocation.get("city") != null ? currentLocation.get("city").toString() : null;

        if (lat == null || lon == null || cityName == null) {
            return buildResultJson("成都", 30.5728, 104.0668);
        }

        // 检查城市是否在数据库中，如果不在则异步添加
        CityInfoEntity cityInfo = cityInfoMapper.selectByCityName(cityName);
        if (cityInfo == null) {
            log.info("IP解析的城市 {} 不在数据库中，异步添加到数据库", cityName);
            asyncAddCityToDatabase(cityName, lat, lon);
        }

        return buildResultJson(cityName, lat, lon);

//        try {
//            List<ParamDataEntity> cities = paramService.getCities();
//            if (cities == null || cities.isEmpty()) {
//                return buildResultJson("成都", 30.5728, 104.0668);
//            }
//
//            String nearestCity = "成都";
//            double nearestLat = 30.5728;
//            double nearestLon = 104.0668;
//            double minDistance = Double.MAX_VALUE;
//
//            for (ParamDataEntity city : cities) {
//                try {
//                    String description = city.getDescription();
//                    if (description == null || description.isEmpty()) {
//                        continue;
//                    }
//
//                    JSONObject cityInfo = JSON.parseObject(description);
//                    Double cityLat = cityInfo.getDouble("latitude");
//                    Double cityLon = cityInfo.getDouble("longitude");
//                    String cityName = cityInfo.getString("city");
//
//                    if (cityLat != null && cityLon != null && cityName != null) {
//                        double distance = LocationUtils.calculateDistance(lat, lon, cityLat, cityLon);
//                        if (distance < minDistance) {
//                            minDistance = distance;
//                            nearestCity = cityName;
//                            nearestLat = cityLat;
//                            nearestLon = cityLon;
//                        }
//                    }
//                } catch (Exception e) {
//                    log.warn("解析城市信息失败: {}", city.getDescription(), e);
//                }
//            }
//
//            return buildResultJson(nearestCity, nearestLat, nearestLon);
//        } catch (Exception e) {
//            log.error("获取最近城市失败", e);
//            return buildResultJson("成都", 30.5728, 104.0668);
//        }
    }

    private String buildResultJson(String city, Double latitude, Double longitude) {
        return String.format("{\"city\":\"%s\",\"latitude\":%s,\"longitude\":%s}", 
                city, latitude, longitude);
    }


    @Tool(description = "判断我的系统里有没有这个城市的数据")
    public Boolean hasThisCity(@ToolParam(description = "这边传入的是当前城市的城市名称") String city){
        CityInfoEntity cityInfo = cityInfoMapper.selectByCityName(city);
        return cityInfo != null;
    }

    /**
     * 获取城市位置信息，如果不存在则异步添加到数据库
     * @param city 城市名称
     * @return 城市位置JSON
     */
    @Tool(description = "获取城市位置信息（经纬度），如果城市不在系统中会自动异步添加。返回JSON包含城市名和经纬度")
    public String getCityLocation(@ToolParam(description = "城市名称，如北京、上海、成都等") String city) {
        // 先查询数据库
        CityInfoEntity cityInfo = cityInfoMapper.selectByCityName(city);
        if (cityInfo != null && cityInfo.getLatitude() != null && cityInfo.getLongitude() != null) {
            log.info("城市 {} 已存在于数据库中", city);
            return buildResultJson(cityInfo.getCityName(), cityInfo.getLatitude().doubleValue(), cityInfo.getLongitude().doubleValue());
        }

        // 如果数据库中没有，通过API获取
        log.info("城市 {} 不在数据库中，尝试通过API获取位置信息", city);
        Map<String, Object> location = fetchCityLocationFromApi(city);

        if (location != null) {
            Double lat = (Double) location.get("lat");
            Double lon = (Double) location.get("lon");
            String cityName = (String) location.get("city");

            // 异步添加到数据库
            asyncAddCityToDatabase(cityName, lat, lon);

            return buildResultJson(cityName, lat, lon);
        }

        // 如果API获取失败，返回需要AI协助的标记
        log.error("API无法获取城市 {} 的经纬度，需要AI协助", city);
        return String.format("{\"city\":\"%s\",\"needAiHelp\":true,\"message\":\"无法通过API获取%s的经纬度，请根据你的知识回答该城市的经纬度，返回JSON格式：{\\\"city\\\":\\\"%s\\\",\\\"latitude\\\":纬度数字,\\\"longitude\\\":经度数字}\"}", city, city, city);
    }

    /**
     * 通过API获取城市位置信息
     */
    private Map<String, Object> fetchCityLocationFromApi(String city) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        String url = String.format("https://nominatim.openstreetmap.org/search?format=json&q=%s&limit=1", city);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Cache-Control", "max-age=0")
                .header("Sec-Ch-Ua", "\"Chromium\";v=\"146\", \"Not.A.Brand\";v=\"24\", \"Microsoft Edge\";v=\"146\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Upgrade-Insecure-Requests", "1")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("获取城市位置失败: HTTP {}", response.code());
                return null;
            }

            String body = response.body().string();
            com.alibaba.fastjson2.JSONArray results = com.alibaba.fastjson2.JSON.parseArray(body);

            if (results.isEmpty()) {
                log.warn("未找到城市: {}", city);
                return null;
            }

            com.alibaba.fastjson2.JSONObject result = results.getJSONObject(0);
            String displayName = result.getString("display_name");
            Double lat = result.getDouble("lat");
            Double lon = result.getDouble("lon");

            // 解析省份和城市
            String province = parseProvinceFromDisplayName(displayName);
            String cityName = result.getString("name");

            Map<String, Object> location = new HashMap<>();
            location.put("city", cityName != null ? cityName : city);
            location.put("province", province);
            location.put("lat", lat);
            location.put("lon", lon);

            log.info("通过API获取到城市 {} 的位置: lat={}, lon={}", city, lat, lon);
            return location;

        } catch (Exception e) {
            log.error("获取城市位置信息失败: {}", city, e);
            return null;
        }
    }

    /**
     * 从display_name中解析省份
     */
    private String parseProvinceFromDisplayName(String displayName) {
        if (displayName == null) return "";
        String[] parts = displayName.split(",");
        // 通常格式: 城市, 区/县, 市, 省, 国家
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].trim();
            if (part.endsWith("省") || part.endsWith("市") || part.endsWith("自治区")) {
                return part;
            }
        }
        return parts.length > 1 ? parts[parts.length - 2].trim() : "";
    }

    /**
     * 异步添加城市到数据库
     */
    private void asyncAddCityToDatabase(String cityName, Double latitude, Double longitude) {
        new Thread(() -> {
            try {
                CityInfoEntity cityInfo = new CityInfoEntity();
                cityInfo.setCityName(cityName);
                cityInfo.setLatitude(java.math.BigDecimal.valueOf(latitude));
                cityInfo.setLongitude(java.math.BigDecimal.valueOf(longitude));
                cityInfo.setCityCode(generateCityCode(cityName));
                cityInfo.setAvailable(1);

                cityInfoMapper.insert(cityInfo);
                log.info("异步添加城市 {} 到数据库成功", cityName);
            } catch (Exception e) {
                log.error("异步添加城市 {} 到数据库失败", cityName, e);
            }
        }).start();
    }

    /**
     * 生成城市编码（简单的拼音首字母+序号）
     */
    private String generateCityCode(String cityName) {
        // 简化为城市名拼音首字母+时间戳后几位
        String prefix = cityName.length() > 0 ? cityName.substring(0, 1) : "X";
        return prefix.toUpperCase() + System.currentTimeMillis() % 10000;
    }

    /**
     * 保存城市信息到数据库（供AI在API获取失败时使用）
     * @param city 城市名称
     * @param latitude 纬度
     * @param longitude 经度
     * @return 是否保存成功
     */
    @Tool(description = "将城市信息保存到数据库。当API无法获取城市经纬度、由AI提供经纬度时调用此方法保存")
    public Boolean saveCityToDatabase(
            @ToolParam(description = "城市名称") String city,
            @ToolParam(description = "纬度") Double latitude,
            @ToolParam(description = "经度") Double longitude) {
        try {
            // 检查是否已存在
            CityInfoEntity existing = cityInfoMapper.selectByCityName(city);
            if (existing != null) {
                log.info("城市 {} 已存在于数据库中，无需重复添加", city);
                return true;
            }

            CityInfoEntity cityInfo = new CityInfoEntity();
            cityInfo.setCityName(city);
            cityInfo.setLatitude(java.math.BigDecimal.valueOf(latitude));
            cityInfo.setLongitude(java.math.BigDecimal.valueOf(longitude));
            cityInfo.setCityCode(generateCityCode(city));
            cityInfo.setAvailable(1);

            cityInfoMapper.insert(cityInfo);
            log.info("AI提供的城市 {} 已保存到数据库", city);
            return true;
        } catch (Exception e) {
            log.error("保存城市 {} 到数据库失败", city, e);
            return false;
        }
    }
}