package com.wf.service;

import com.wf.object.entity.WeatherDataEntity;

import java.util.List;

public interface WeatherDataService {

    List<WeatherDataEntity> fetchWeatherData(String apiUrl);

    void saveWeatherData(List<WeatherDataEntity> weatherDataList);

    void manualFetchWeatherData(Integer beginTime,Integer endTime);

    void predictWeatherData();
}
