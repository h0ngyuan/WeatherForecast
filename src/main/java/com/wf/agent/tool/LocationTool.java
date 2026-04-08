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
    @Tool(description = "获取离当前位置最近的可预测城市（返回JSON包含城市名和经纬度）")
    public String getNearestAvailableCity() throws UnknownHostException {
        Map<String, Object> currentLocation = LocationUtils.getCurrentLocationMap();
        if (currentLocation == null) {
            return buildResultJson("成都", 30.5728, 104.0668);
        }

        Double lat = (Double) currentLocation.get("lat");
        Double lon = (Double) currentLocation.get("lon");

        if (lat == null || lon == null) {
            return buildResultJson("成都", 30.5728, 104.0668);
        }
        return buildResultJson(currentLocation.get("city").toString(),(Double) currentLocation.get("lat"),(Double) currentLocation.get("lon"));

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
}