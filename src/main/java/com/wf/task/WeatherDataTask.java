package com.wf.task;

import com.wf.object.entity.WeatherDataEntity;
import com.wf.service.WeatherDataService;
import com.wf.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class WeatherDataTask {

    private static final String WEATHER_FETCH_API = "https://archive-api.open-meteo.com/v1/archive?" +
            "latitude=30.9647&longitude=103.6258&hourly=temperature_2m," +
            "relative_humidity_2m,dew_point_2m,precipitation,rain,snowfall,apparent_temperature,weather_code," +
            "pressure_msl,surface_pressure,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m," +
            "soil_temperature_0_to_7cm,snow_depth,cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
            "et0_fao_evapotranspiration,vapour_pressure_deficit,wind_speed_100m,wind_direction_100m," +
            "soil_temperature_7_to_28cm,soil_moisture_0_to_7cm,soil_moisture_7_to_28cm&timezone=Asia%2FSingapore";

    @Autowired
    private WeatherDataService weatherDataService;

    @Scheduled(cron = "5 0 0 1/1 * ? ")
    @Transactional(rollbackFor = Exception.class)
    public void periodicalFetchFormalData() {
        try {
            String api = WEATHER_FETCH_API + "&start_date=" + TimeUtils.acquirePastFormatTime(2, TimeUnit.DAYS) +
                    "&end_date=" + TimeUtils.acquirePastFormatTime(1, TimeUnit.DAYS);
            
            log.info("开始执行定时任务，获取天气数据，API: {}", api);
            
            List<WeatherDataEntity> weatherDataList = weatherDataService.fetchWeatherData(api);
            weatherDataService.saveWeatherData(weatherDataList);
            
            log.info("定时任务执行完成，成功保存天气数据");
            
        } catch (Exception e) {
            log.error("气象数据定时任务失败", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new RuntimeException("气象数据定时任务失败", e);
        }
    }

    @Scheduled(cron = "10 0 0 1/1 * ? ")
    @Transactional(rollbackFor = Exception.class)
    public void periodicalFetchPredictedData() {
        try {
            log.info("开始执行天气预测定时任务");
            weatherDataService.predictWeatherData();
            log.info("天气预测定时任务执行完成");
        } catch (Exception e) {
            log.error("天气预测定时任务失败", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new RuntimeException("天气预测定时任务失败", e);
        }
    }

    public static void main(String[] args) {
    }
}