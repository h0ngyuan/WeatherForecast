package com.wf.service;

import com.wf.object.query.WeatherCodeQuery;

import java.util.List;

public interface WeatherForecastService {

    List<String> acquireWeatherCodeValueByRangeTime(WeatherCodeQuery query);
}
