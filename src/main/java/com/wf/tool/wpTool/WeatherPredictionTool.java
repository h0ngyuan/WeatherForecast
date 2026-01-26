package com.wf.tool.wpTool;

import com.wf.object.query.WeatherCodeQuery;

import java.util.List;

public interface WeatherPredictionTool {

    List<String> acquireWeatherCodeValueByRangeTime(WeatherCodeQuery query);

    List<Integer> predictNext24Hours(WeatherCodeQuery query);

    List<Integer> predictNext72Hours(WeatherCodeQuery query);

}
