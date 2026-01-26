package com.wf.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wf.mapper.WeatherDataMapper;
import com.wf.object.entity.WeatherDataEntity;
import com.wf.service.WeatherDataService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WeatherDataServiceImpl implements WeatherDataService {

    private static final String LOCATION = "成都";

    @Autowired
    private WeatherDataMapper weatherDataMapper;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public List<WeatherDataEntity> fetchWeatherData(String apiUrl) {
        try {
            log.info("开始获取天气数据，API: {}", apiUrl);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new RuntimeException("API 调用失败: " + response);
            }

            String responseBody = response.body().string();
            JSONObject jsonObject = JSON.parseObject(responseBody);

            JSONObject hourly = jsonObject.getJSONObject("hourly");
            JSONArray timeArray = hourly.getJSONArray("time");
            JSONArray tempArray = hourly.getJSONArray("temperature_2m");
            JSONArray rhArray = hourly.getJSONArray("relative_humidity_2m");
            JSONArray dewArray = hourly.getJSONArray("dew_point_2m");
            JSONArray precipArray = hourly.getJSONArray("precipitation");
            JSONArray rainArray = hourly.getJSONArray("rain");
            JSONArray snowArray = hourly.getJSONArray("snowfall");
            JSONArray appTempArray = hourly.getJSONArray("apparent_temperature");
            JSONArray weatherCodeArray = hourly.getJSONArray("weather_code");
            JSONArray pMslArray = hourly.getJSONArray("pressure_msl");
            JSONArray surfPArray = hourly.getJSONArray("surface_pressure");
            JSONArray cloudArray = hourly.getJSONArray("cloud_cover");
            JSONArray wind10Array = hourly.getJSONArray("wind_speed_10m");
            JSONArray dir10Array = hourly.getJSONArray("wind_direction_10m");
            JSONArray gust10Array = hourly.getJSONArray("wind_gusts_10m");
            JSONArray soilT0Array = hourly.getJSONArray("soil_temperature_0_to_7cm");
            JSONArray snowDepthArray = hourly.getJSONArray("snow_depth");
            JSONArray cloudLowArray = hourly.getJSONArray("cloud_cover_low");
            JSONArray cloudMidArray = hourly.getJSONArray("cloud_cover_mid");
            JSONArray cloudHighArray = hourly.getJSONArray("cloud_cover_high");
            JSONArray et0Array = hourly.getJSONArray("et0_fao_evapotranspiration");
            JSONArray vpdArray = hourly.getJSONArray("vapour_pressure_deficit");
            JSONArray wind100Array = hourly.getJSONArray("wind_speed_100m");
            JSONArray dir100Array = hourly.getJSONArray("wind_direction_100m");
            JSONArray soilT7Array = hourly.getJSONArray("soil_temperature_7_to_28cm");
            JSONArray soilM0Array = hourly.getJSONArray("soil_moisture_0_to_7cm");
            JSONArray soilM7Array = hourly.getJSONArray("soil_moisture_7_to_28cm");

            List<WeatherDataEntity> entityList = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            for (int i = 0; i < timeArray.size(); i++) {
                WeatherDataEntity entity = new WeatherDataEntity();
                entity.setLocation(LOCATION);

                String timeStr = timeArray.getString(i);
                entity.setTime(LocalDateTime.parse(timeStr, formatter));

                entity.setTemp(getBigDecimal(tempArray, i));
                entity.setRh(getBigDecimal(rhArray, i));
                entity.setDew(getBigDecimal(dewArray, i));
                entity.setPrecip(getBigDecimal(precipArray, i));
                entity.setRain(getBigDecimal(rainArray, i));
                entity.setSnow(getBigDecimal(snowArray, i));
                entity.setAppTemp(getBigDecimal(appTempArray, i));
                entity.setWeatherCode(getInteger(weatherCodeArray, i));
                entity.setPMsl(getBigDecimal(pMslArray, i));
                entity.setSurfP(getBigDecimal(surfPArray, i));
                entity.setCloud(getBigDecimal(cloudArray, i));
                entity.setWind10(getBigDecimal(wind10Array, i));
                entity.setDir10(getInteger(dir10Array, i));
                entity.setGust10(getBigDecimal(gust10Array, i));
                entity.setSoilT0(getBigDecimal(soilT0Array, i));
                entity.setSnowDepth(getBigDecimal(snowDepthArray, i));
                entity.setCloudLow(getBigDecimal(cloudLowArray, i));
                entity.setCloudMid(getBigDecimal(cloudMidArray, i));
                entity.setCloudHigh(getBigDecimal(cloudHighArray, i));
                entity.setEt0(getBigDecimal(et0Array, i));
                entity.setVpd(getBigDecimal(vpdArray, i));
                entity.setWind100(getBigDecimal(wind100Array, i));
                entity.setDir100(getInteger(dir100Array, i));
                entity.setSoilT7(getBigDecimal(soilT7Array, i));
                entity.setSoilM0(getBigDecimal(soilM0Array, i));
                entity.setSoilM7(getBigDecimal(soilM7Array, i));

                entity.setAvailable(1);
                entity.setCreateTime(LocalDateTime.now());
                entity.setUpdateTime(LocalDateTime.now());

                entityList.add(entity);
            }

            log.info("成功获取 {} 条天气数据", entityList.size());
            return entityList;

        } catch (Exception e) {
            log.error("获取天气数据失败", e);
            throw new RuntimeException("获取天气数据失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWeatherData(List<WeatherDataEntity> weatherDataList) {
        try {
            if (weatherDataList == null || weatherDataList.isEmpty()) {
                log.warn("天气数据列表为空，跳过保存");
                return;
            }

            for (WeatherDataEntity entity : weatherDataList) {
                weatherDataMapper.insert(entity);
            }

            log.info("成功保存 {} 条天气数据到数据库", weatherDataList.size());

        } catch (Exception e) {
            log.error("保存天气数据到数据库失败", e);
            throw new RuntimeException("保存天气数据到数据库失败", e);
        }
    }

    private BigDecimal getBigDecimal(JSONArray array, int index) {
        if (array == null || index >= array.size()) {
            return null;
        }
        Object value = array.get(index);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return null;
    }

    private Integer getInteger(JSONArray array, int index) {
        if (array == null || index >= array.size()) {
            return null;
        }
        Object value = array.get(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}
