package com.wf.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.agent.map.WeatherImpactAgent;
import com.wf.agent.map.entity.CityWeatherDaily;
import com.wf.mapper.CityWeatherDailyMapper;
import com.wf.mapper.CityInfoMapper;
import com.wf.mapper.PredictWeatherCodeMapper;
import com.wf.mapper.WeatherDataMapper;
import com.wf.object.entity.CityInfoEntity;
import com.wf.object.entity.PredictWeatherCodeEntity;
import com.wf.object.entity.WeatherDataEntity;
import com.wf.service.WeatherDataService;
import com.wf.utils.TimeUtils;
import com.wf.utils.WeatherCodeCache;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WeatherDataServiceImpl implements WeatherDataService {

    private static final String LOCATION = "成都";
    private static final String PREDICT_API_URL = "http://119.91.238.231:8000/predict/wmo";

    @Autowired
    private WeatherDataMapper weatherDataMapper;

    @Autowired
    private PredictWeatherCodeMapper predictWeatherCodeMapper;

    @Autowired
    private CityWeatherDailyMapper cityWeatherDailyMapper;

    @Autowired
    private CityInfoMapper cityInfoMapper;

    @Autowired
    private WeatherImpactAgent weatherImpactAgent;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private WeatherDataService self;

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
                log.info("成功插入一条数据");
            }

            log.info("成功保存 {} 条天气数据到数据库", weatherDataList.size());

        } catch (Exception e) {
            log.error("保存天气数据到数据库失败", e);
            throw new RuntimeException("保存天气数据到数据库失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualFetchWeatherData(Integer beginTime,Integer endTime) {
        try {
            String api = "https://archive-api.open-meteo.com/v1/archive?" +
                    "latitude=30.9647&longitude=103.6258&hourly=temperature_2m," +
                    "relative_humidity_2m,dew_point_2m,precipitation,rain,snowfall,apparent_temperature,weather_code," +
                    "pressure_msl,surface_pressure,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m," +
                    "soil_temperature_0_to_7cm,snow_depth,cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
                    "et0_fao_evapotranspiration,vapour_pressure_deficit,wind_speed_100m,wind_direction_100m," +
                    "soil_temperature_7_to_28cm,soil_moisture_0_to_7cm,soil_moisture_7_to_28cm&timezone=Asia%2FSingapore" +
                    "&start_date=" + TimeUtils.acquirePastFormatTime(beginTime, TimeUnit.DAYS).substring(0,10) +
                    "&end_date=" + TimeUtils.acquirePastFormatTime(endTime, TimeUnit.DAYS).substring(0,10);
            
            log.info("开始手动执行天气数据获取，API: {}", api);
            
            List<WeatherDataEntity> weatherDataList = fetchWeatherData(api);
            saveWeatherData(weatherDataList);
            
            log.info("手动执行完成，成功保存 {} 条天气数据", weatherDataList.size());
            
        } catch (Exception e) {
            log.error("手动获取天气数据失败", e);
            throw new RuntimeException("手动获取天气数据失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void predictWeatherData() {
        try {
            log.info("开始执行天气预测任务");

//            String beginTime = TimeUtils.acquirePastFormatTime(169, TimeUnit.HOURS);
            String beginTime = "2026-01-31 23:00:00";

            List<WeatherDataEntity> historyData = weatherDataMapper.selectEntitys(beginTime,LOCATION);
            if (historyData.size() < 168) {
                log.warn("历史数据不足，需要168小时，实际只有 {} 小时", historyData.size());
                return;
            }

            log.info("成功获取 {} 小时历史数据", historyData.size());

            Map<String, Object> hourly = new HashMap<>();
            List<String> timeList = new ArrayList<>();
            List<BigDecimal> tempList = new ArrayList<>();
            List<BigDecimal> rhList = new ArrayList<>();
            List<BigDecimal> dewList = new ArrayList<>();
            List<BigDecimal> precipList = new ArrayList<>();
            List<BigDecimal> rainList = new ArrayList<>();
            List<BigDecimal> snowList = new ArrayList<>();
            List<BigDecimal> appTempList = new ArrayList<>();
            List<Integer> weatherCodeList = new ArrayList<>();
            List<BigDecimal> pMslList = new ArrayList<>();
            List<BigDecimal> surfPList = new ArrayList<>();
            List<BigDecimal> cloudList = new ArrayList<>();
            List<BigDecimal> wind10List = new ArrayList<>();
            List<Integer> dir10List = new ArrayList<>();
            List<BigDecimal> gust10List = new ArrayList<>();
            List<BigDecimal> soilT0List = new ArrayList<>();
            List<BigDecimal> snowDepthList = new ArrayList<>();
            List<BigDecimal> cloudLowList = new ArrayList<>();
            List<BigDecimal> cloudMidList = new ArrayList<>();
            List<BigDecimal> cloudHighList = new ArrayList<>();
            List<BigDecimal> et0List = new ArrayList<>();
            List<BigDecimal> vpdList = new ArrayList<>();
            List<BigDecimal> wind100List = new ArrayList<>();
            List<Integer> dir100List = new ArrayList<>();
            List<BigDecimal> soilT7List = new ArrayList<>();
            List<BigDecimal> soilM0List = new ArrayList<>();
            List<BigDecimal> soilM7List = new ArrayList<>();

            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            for (WeatherDataEntity entity : historyData) {
                timeList.add(entity.getTime().format(timeFormatter));
                tempList.add(entity.getTemp());
                rhList.add(entity.getRh());
                dewList.add(entity.getDew());
                precipList.add(entity.getPrecip());
                rainList.add(entity.getRain());
                snowList.add(entity.getSnow());
                appTempList.add(entity.getAppTemp());
                weatherCodeList.add(entity.getWeatherCode());
                pMslList.add(entity.getPMsl());
                surfPList.add(entity.getSurfP());
                cloudList.add(entity.getCloud());
                wind10List.add(entity.getWind10());
                dir10List.add(entity.getDir10());
                gust10List.add(entity.getGust10());
                soilT0List.add(entity.getSoilT0());
                snowDepthList.add(entity.getSnowDepth());
                cloudLowList.add(entity.getCloudLow());
                cloudMidList.add(entity.getCloudMid());
                cloudHighList.add(entity.getCloudHigh());
                et0List.add(entity.getEt0());
                vpdList.add(entity.getVpd());
                wind100List.add(entity.getWind100());
                dir100List.add(entity.getDir100());
                soilT7List.add(entity.getSoilT7());
                soilM0List.add(entity.getSoilM0());
                soilM7List.add(entity.getSoilM7());
            }

            hourly.put("time", timeList);
            hourly.put("temperature_2m", tempList);
            hourly.put("relative_humidity_2m", rhList);
            hourly.put("dew_point_2m", dewList);
            hourly.put("precipitation", precipList);
            hourly.put("rain", rainList);
            hourly.put("snowfall", snowList);
            hourly.put("apparent_temperature", appTempList);
            hourly.put("weather_code", weatherCodeList);
            hourly.put("pressure_msl", pMslList);
            hourly.put("surface_pressure", surfPList);
            hourly.put("cloud_cover", cloudList);
            hourly.put("wind_speed_10m", wind10List);
            hourly.put("wind_direction_10m", dir10List);
            hourly.put("wind_gusts_10m", gust10List);
            hourly.put("soil_temperature_0_to_7cm", soilT0List);
            hourly.put("snow_depth", snowDepthList);
            hourly.put("cloud_cover_low", cloudLowList);
            hourly.put("cloud_cover_mid", cloudMidList);
            hourly.put("cloud_cover_high", cloudHighList);
            hourly.put("et0_fao_evapotranspiration", et0List);
            hourly.put("vapour_pressure_deficit", vpdList);
            hourly.put("wind_speed_100m", wind100List);
            hourly.put("wind_direction_100m", dir100List);
            hourly.put("soil_temperature_7_to_28cm", soilT7List);
            hourly.put("soil_moisture_0_to_7cm", soilM0List);
            hourly.put("soil_moisture_7_to_28cm", soilM7List);

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("hourly", hourly);

            String jsonRequest = JSON.toJSONString(requestData);
            log.info("发送预测请求到API: {}", PREDICT_API_URL);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonRequest
            );

            Request request = new Request.Builder()
                    .url(PREDICT_API_URL)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();

            String responseBody = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);

            JSONArray predictions = jsonResponse.getJSONArray("predictions");
            List<Integer> predictionList = new ArrayList<>();
            for (int i = 0; i < predictions.size(); i++) {
                predictionList.add(predictions.getInteger(i));
            }

            log.info("成功获取 {} 条预测数据", predictionList.size());

            String baseTimeStr = TimeUtils.getCurrentFormatTime();
            LocalDateTime baseTime = LocalDateTime.parse(baseTimeStr, TimeUtils.FORMATTER);

            for (int i = 0; i < predictionList.size(); i++) {
                PredictWeatherCodeEntity entity = new PredictWeatherCodeEntity();
                entity.setLocation(LOCATION);
                entity.setTime(baseTime.plusHours(i));
                entity.setWeatherCode(predictionList.get(i));
                entity.setWeatherCodeValue(WeatherCodeCache.getWeatherCodeValue(predictionList.get(i)));
                entity.setSourceType(0);
                entity.setAvailable(1);
                entity.setCreateTime(LocalDateTime.now());
                entity.setUpdateTime(LocalDateTime.now());

                predictWeatherCodeMapper.insert(entity);
            }

            log.info("天气预测任务执行完成，成功保存 {} 条预测数据", predictionList.size());

            // 通过代理调用异步方法，确保 @Async 生效
            self.saveToCityWeatherDailyAsync(baseTime.toLocalDate(), predictionList);

        } catch (Exception e) {
            log.error("天气预测任务失败", e);
            throw new RuntimeException("天气预测任务失败", e);
        }
    }

    /**
     * 异步保存预测数据到 city_weather_daily 表，并协同Agent分析周边影响
     */
    @Async
    public void saveToCityWeatherDailyAsync(LocalDate recordDate, List<Integer> predictionList) {
        try {
            log.info("[Async] 开始保存到 city_weather_daily, date={}", recordDate);

            // 获取所有城市
            List<CityInfoEntity> cities = cityInfoMapper.selectList(null);
            if (cities.isEmpty()) {
                log.warn("[Async] 没有城市数据，跳过保存");
                return;
            }

            // 第一步：为所有城市保存基础数据
            for (CityInfoEntity city : cities) {
                // 检查是否已存在
                CityWeatherDaily existing = cityWeatherDailyMapper.selectByCityAndDate(city.getCityCode(), recordDate);
                if (existing != null) {
                    log.debug("[Async] 城市 {} 的 {} 数据已存在，跳过", city.getCityName(), recordDate);
                    continue;
                }

                CityWeatherDaily daily = new CityWeatherDaily();
                daily.setCityCode(city.getCityCode());
                daily.setCityName(city.getCityName());
                daily.setRecordDate(recordDate);
                daily.setHourlyWeatherCodes(predictionList.toString());
                daily.setHasDisaster(0); // 默认无灾害
                daily.setCreateTime(LocalDateTime.now());
                daily.setUpdateTime(LocalDateTime.now());

                cityWeatherDailyMapper.insert(daily);
                log.debug("[Async] 保存城市 {} 的日天气数据成功", city.getCityName());
            }

            log.info("[Async] 完成基础数据保存，开始Agent协同分析周边影响");

            // 第二步：Agent协同分析周边影响（半径200公里）
            for (CityInfoEntity city : cities) {
                try {
                    WeatherImpactAgent.ImpactAnalysisResult result = 
                        weatherImpactAgent.analyzeImpact(city.getCityCode(), city.getCityName(), recordDate, 200);

                    if (result.isHasImpact()) {
                        // 更新该城市的灾害信息
                        CityWeatherDaily daily = cityWeatherDailyMapper.selectByCityAndDate(city.getCityCode(), recordDate);
                        if (daily != null) {
                            daily.setHasDisaster(1);
                            daily.setMaxDisasterLevel(result.getSuggestedLevel());
                            daily.setDisasterTypes("周边传播风险");
                            daily.setUpdateTime(LocalDateTime.now());
                            cityWeatherDailyMapper.updateById(daily);
                            log.info("[Async] 城市 {} 受周边影响，更新灾害等级: {}",
                                city.getCityName(), result.getSuggestedLevel());
                        }
                    }
                } catch (Exception e) {
                    log.warn("[Async] 分析城市 {} 周边影响失败: {}", city.getCityName(), e.getMessage());
                }
            }

            log.info("[Async] 完成Agent协同分析，date={}", recordDate);
        } catch (Exception e) {
            log.error("[Async] 保存到 city_weather_daily 失败", e);
        }
    }

    @Override
    @Async
    public void saveSingleCityToDailyAsync(String cityName, String weatherCodesStr) {
        try {
            log.info("[Async][SingleCity] 开始写入 city_weather_daily: city={}", cityName);

            if (weatherCodesStr == null || weatherCodesStr.isEmpty()) {
                log.warn("[Async][SingleCity] 天气码为空，跳过: city={}", cityName);
                return;
            }

            LocalDate today = LocalDate.now();
            CityInfoEntity cityInfo = cityInfoMapper.selectByCityName(cityName);
            if (cityInfo == null) {
                log.warn("[Async][SingleCity] 未找到城市信息: city={}", cityName);
                return;
            }

            if (cityInfo.getCityCode() == null || cityInfo.getCityCode().isEmpty()) {
                log.warn("[Async][SingleCity] 城市编码为空，跳过: city={}", cityName);
                return;
            }

            CityWeatherDaily existing = cityWeatherDailyMapper.selectByCityAndDate(cityInfo.getCityCode(), today);
            if (existing != null) {
                log.info("[Async][SingleCity] 数据已存在，跳过: city={}, date={}", cityName, today);
                return;
            }

            CityWeatherDaily daily = new CityWeatherDaily();
            daily.setCityCode(cityInfo.getCityCode());
            daily.setCityName(cityInfo.getCityName());
            daily.setRecordDate(today);
            daily.setHourlyWeatherCodes(weatherCodesStr);

            // 解析天气码，判断灾害
            int[] dayCode = {0};
            int[] maxDisasterLevel = {0};
            boolean[] hasDisaster = {false};
            List<String> disasterTypes = new ArrayList<>();

            try {
                String[] codes = weatherCodesStr.split(",");
                if (codes.length > 0) {
                    dayCode[0] = Integer.parseInt(codes[0].trim());
                }
                daily.setDayWeatherCode(dayCode[0]);

                for (String codeStr : codes) {
                    int code = Integer.parseInt(codeStr.trim());
                    int level = getDisasterLevel(code);
                    if (level > 0) {
                        hasDisaster[0] = true;
                        if (level > maxDisasterLevel[0]) {
                            maxDisasterLevel[0] = level;
                        }
                        String type = getDisasterType(code);
                        if (!disasterTypes.contains(type)) {
                            disasterTypes.add(type);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("[Async][SingleCity] 天气码解析失败: city={}, codes={}", cityName, weatherCodesStr);
            }

            daily.setHasDisaster(hasDisaster[0] ? 1 : 0);
            daily.setMaxDisasterLevel(maxDisasterLevel[0]);
            if (!disasterTypes.isEmpty()) {
                daily.setDisasterTypes(String.join(",", disasterTypes));
            }

            daily.setCreateTime(LocalDateTime.now());
            daily.setUpdateTime(LocalDateTime.now());

            log.info("[Async][SingleCity] 写入 city_weather_daily: city={}, dayCode={}, hasDisaster={}, maxLevel={}",
                    cityName, dayCode[0], hasDisaster[0], maxDisasterLevel[0]);

            int rows = cityWeatherDailyMapper.insert(daily);
            if (rows > 0) {
                log.info("[Async][SingleCity] 写入成功: city={}, date={}, id={}", cityName, today, daily.getId());
            } else {
                log.warn("[Async][SingleCity] insert 返回 0: city={}", cityName);
            }
        } catch (Exception e) {
            log.error("[Async][SingleCity] 写入 city_weather_daily 失败: city={}", cityName, e);
        }
    }

    private int getDisasterLevel(int code) {
        return switch (code) {
            case 49, 48 -> 1;
            case 47, 15, 33 -> 2;
            case 46 -> 3;
            default -> 0;
        };
    }

    private String getDisasterType(int code) {
        return switch (code) {
            case 49 -> "暴雨";
            case 48 -> "大雨";
            case 47 -> "中雨";
            case 46 -> "小雨";
            case 15 -> "雷阵雨";
            case 33 -> "雾";
            case 75 -> "霾";
            default -> "未知";
        };
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
