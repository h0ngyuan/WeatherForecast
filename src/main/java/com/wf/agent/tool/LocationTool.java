package com.wf.agent.tool;

import cn.dev33.satoken.context.SaHolder;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wf.object.entity.ParamDataEntity;
import com.wf.service.ParamService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LocationTool {

    @Autowired
    private ParamService paramService;

    /**
     * 获取离当前位置最近的可预测城市
     * @return 最近城市的名称
     */
    @Tool(description = "获取离当前位置最近的可预测城市")
    public String getNearestAvailableCity() {
        Map<String, Object> currentLocation = LocationUtils.getCurrentLocationMap();
        if (currentLocation == null) {
            return "成都";
        }

        Double lat = (Double) currentLocation.get("lat");
        Double lon = (Double) currentLocation.get("lon");

        if (lat == null || lon == null) {
            return "成都";
        }

        try {
            List<ParamDataEntity> cities = paramService.getCities();
            if (cities == null || cities.isEmpty()) {
                return "成都";
            }

            // 默认返回成都
            String nearestCity = "成都";
            double minDistance = Double.MAX_VALUE;

            // 遍历所有城市，计算距离
            for (ParamDataEntity city : cities) {
                try {
                    String description = city.getDescription();
                    if (description == null || description.isEmpty()) {
                        continue;
                    }

                    JSONObject cityInfo = JSON.parseObject(description);
                    Double cityLat = cityInfo.getDouble("latitude");
                    Double cityLon = cityInfo.getDouble("longitude");
                    String cityName = cityInfo.getString("city");

                    if (cityLat != null && cityLon != null && cityName != null) {
                        double distance = LocationUtils.calculateDistance(lat, lon, cityLat, cityLon);
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestCity = cityName;
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析城市信息失败: {}", city.getDescription(), e);
                }
            }

            return nearestCity;
        } catch (Exception e) {
            log.error("获取最近城市失败", e);
            return "成都";
        }
    }


    @Tool(description = "判断我的系统里有没有这个城市的数据")
    public Boolean hasThisCity(@ToolParam(description = "这边传入的是当前城市的城市名称") String city){
        List<ParamDataEntity> cities = paramService.getCities();
        Set<String> set = (Set<String>) cities.stream().map((Function<? super ParamDataEntity, ?>) c -> JSONObject.parseObject(c.getDescription()).get("city").toString()).collect(Collectors.toSet());
        return set.contains(city);
    }
}