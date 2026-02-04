package com.wf.utils;

import cn.dev33.satoken.context.SaHolder;
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
    public static Map<String, Object> getCurrentLocationMap() {
        String ip = getIp();
        if (ip == null || ip.isEmpty()) {
            return getDefaultGpsInfo();
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
}
