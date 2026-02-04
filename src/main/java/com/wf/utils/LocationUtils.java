package com.wf.utils;


import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LocationUtils {

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    public static Map<String, Object> getGpsInfo() {
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

    private static Map<String, Object> getDefaultGpsInfo() {
        Map<String, Object> defaultInfo = new HashMap<>();
        defaultInfo.put("city", "成都");
        defaultInfo.put("lat", 30.5728);
        defaultInfo.put("lon", 104.0668);
        return defaultInfo;
    }

    public static String getIp() {
        return ((Map<String, Object>) SaHolder.getStorage().get("requestInfo")).get("remoteHost").toString();
    }
}
