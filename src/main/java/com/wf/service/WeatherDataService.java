package com.wf.service;

import com.wf.object.entity.WeatherDataEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface WeatherDataService {

    List<WeatherDataEntity> fetchWeatherData(String apiUrl);

    void saveWeatherData(List<WeatherDataEntity> weatherDataList);

    void manualFetchWeatherData(Integer beginTime,Integer endTime);

    void predictWeatherData();

    void saveToCityWeatherDailyAsync(LocalDate recordDate, List<Integer> predictionList);

    void saveSingleCityToDailyAsync(String cityName, String weatherCodesStr);
}
