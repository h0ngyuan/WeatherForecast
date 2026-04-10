package com.wf.utils;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wf.object.entity.ParamDataEntity;
import com.wf.service.ParamService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
public class LocationUtils {

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    /**
     * 获取当前地点城市坐标
     * @return
     */
    @NotNull
    public static Map<String, Object> getCurrentLocationMap() throws UnknownHostException {
        String ip = getIp();
        log.info("请求IP"+ip);
        if (ip == null || ip.isEmpty()) {
            return getDefaultGpsInfo();
        }
        if (ip.equals("127.0.0.1")||ip.equals("0:0:0:0:0:0:0:1")){
            InetAddress localHost = InetAddress.getLocalHost();
            ip = localHost.getHostAddress();
            log.info("遇到本机，IP改成"+ip);
        }
        try {
            String url = "http://ip-api.com/json/" + ip + "?lang=zh-CN";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            String responseBody = "";
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() != null) {
                    responseBody = response.body().string();
                }
            }
            JSONObject json = JSON.parseObject(responseBody);
            log.info("这个是IP解析请求的返回值"+json.toJSONString());
            Map<String, Object> result = new HashMap<>();
            result.put("city", json.getString("city"));
            result.put("lat", json.getDouble("lat"));
            result.put("lon", json.getDouble("lon"));
            return result;
        } catch (IOException ignored) {
        } catch (Exception e) {
            return getDefaultGpsInfo();
        }
        return getDefaultGpsInfo();
    }

    public static Map<String, Object> getDefaultGpsInfo() {
        Map<String, Object> defaultInfo = new HashMap<>();
        defaultInfo.put("city", "成都");
        defaultInfo.put("lat", 30.5728);
        defaultInfo.put("lon", 104.0668);
        return defaultInfo;
    }

    public static String getIp() {
        Object requestInfo = SaHolder.getStorage().get("requestInfo");
        log.info("请求IP全"+JSONObject.toJSONString(requestInfo));
        return ((Map<String, Object>) SaHolder.getStorage().get("requestInfo")).get("remoteHost").toString();
    }

    /**
     * 计算两个经纬度点之间的距离（单位：公里）
     * 使用Haversine公式
     * @param lat1 点1的纬度
     * @param lon1 点1的经度
     * @param lat2 点2的纬度
     * @param lon2 点2的经度
     * @return 两点之间的距离（公里）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 地球半径，单位：公里
        final int R = 6371;

        // 将经纬度转换为弧度
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        // 应用Haversine公式
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 根据城市名称获取地理位置坐标（使用 OpenStreetMap Nominatim）
     * @param cityName 城市名称
     * @return Map包含 lat（纬度）和 lon（经度），如果未找到返回 null
     */
    public static Map<String, Double> getCoordinatesByCityName(String cityName) {
        if (cityName == null || cityName.isEmpty()) {
            log.warn("城市名称为空");
            return null;
        }

        try {
            // Nominatim API 需要进行 URL 编码
            String encodedCity = java.net.URLEncoder.encode(cityName, "UTF-8");
            String url = String.format(
                "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1",
                encodedCity
            );

            Request request = new Request.Builder()
                    .url(url)
//                    .header("User-Agent", "WeatherForecastApp/1.0")  // Nominatim 要求必须提供 User-Agent
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Nominatim API 请求失败: {}", response.code());
                    return null;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                if (responseBody.isEmpty()) {
                    log.warn("Nominatim API 返回空响应");
                    return null;
                }

                // 解析 JSON 响应
                com.alibaba.fastjson2.JSONArray results = JSON.parseArray(responseBody);
                if (results == null || results.isEmpty()) {
                    log.warn("未找到城市 '{}' 的坐标信息", cityName);
                    return null;
                }

                JSONObject firstResult = results.getJSONObject(0);
                Double lat = firstResult.getDouble("lat");
                Double lon = firstResult.getDouble("lon");

                if (lat == null || lon == null) {
                    log.warn("城市 '{}' 的坐标信息不完整", cityName);
                    return null;
                }

                Map<String, Double> coordinates = new HashMap<>();
                coordinates.put("lat", lat);
                coordinates.put("lon", lon);

                log.info("成功获取城市 '{}' 的坐标: lat={}, lon={}", cityName, lat, lon);
                return coordinates;
            }
        } catch (IOException e) {
            log.error("获取城市 '{}' 坐标时发生 IO 异常: {}", cityName, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("获取城市 '{}' 坐标时发生异常: {}", cityName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据IP地址获取城市名称
     * 使用 ip-api.com 免费接口
     *
     * @param ip IP地址
     * @return 城市名称，失败返回 null
     */
    public static String getCityByIp(String ip) {
        if (ip == null || ip.isEmpty() || "127.0.0.1".equals(ip) || "localhost".equals(ip)) {
            log.debug("[LocationUtils] 本地IP或空IP，返回默认城市");
            return null;
        }

        try {
            String url = "http://ip-api.com/json/" + ip + "?lang=zh-CN";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("[LocationUtils] IP查询请求失败: {}", response.code());
                    return null;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                if (responseBody.isEmpty()) {
                    log.warn("[LocationUtils] IP查询返回空响应");
                    return null;
                }

                JSONObject json = JSON.parseObject(responseBody);
                String status = json.getString("status");

                if (!"success".equals(status)) {
                    log.warn("[LocationUtils] IP查询失败: {}", json.getString("message"));
                    return null;
                }

                String city = json.getString("city");
                log.info("[LocationUtils] IP {} 对应城市: {}", ip, city);
                return city;
            }
        } catch (IOException e) {
            log.error("[LocationUtils] IP查询IO异常: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[LocationUtils] IP查询异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据IP地址获取完整位置信息（包含城市、经纬度）
     * 使用 ip-api.com 免费接口
     *
     * @param ip IP地址
     * @return 包含city、lat、lon的Map，失败返回默认城市信息
     */
    public static Map<String, Object> getLocationMapByIp(String ip) {
        if (ip == null || ip.isEmpty() || "127.0.0.1".equals(ip) || "localhost".equals(ip)) {
            log.debug("[LocationUtils] 本地IP或空IP，返回默认位置信息");
            return getDefaultGpsInfo();
        }

        try {
            String url = "http://ip-api.com/json/" + ip + "?lang=zh-CN";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("[LocationUtils] IP查询请求失败: {}", response.code());
                    return getDefaultGpsInfo();
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                if (responseBody.isEmpty()) {
                    log.warn("[LocationUtils] IP查询返回空响应");
                    return getDefaultGpsInfo();
                }

                JSONObject json = JSON.parseObject(responseBody);
                String status = json.getString("status");

                if (!"success".equals(status)) {
                    log.warn("[LocationUtils] IP查询失败: {}", json.getString("message"));
                    return getDefaultGpsInfo();
                }

                Map<String, Object> result = new HashMap<>();
                result.put("city", json.getString("city"));
                result.put("lat", json.getDouble("lat"));
                result.put("lon", json.getDouble("lon"));
                log.info("[LocationUtils] IP {} 对应位置: city={}, lat={}, lon={}",
                        ip, result.get("city"), result.get("lat"), result.get("lon"));
                return result;
            }
        } catch (IOException e) {
            log.error("[LocationUtils] IP查询IO异常: {}", e.getMessage());
            return getDefaultGpsInfo();
        } catch (Exception e) {
            log.error("[LocationUtils] IP查询异常: {}", e.getMessage(), e);
            return getDefaultGpsInfo();
        }
    }

    public static void main(String[] args) {
        Map<String, Double> city = getCoordinatesByCityName("南通");
        System.out.println(city);
    }
}
