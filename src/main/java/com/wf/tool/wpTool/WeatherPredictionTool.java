package com.wf.tool.wpTool;

public interface WeatherPredictionTool {

    String predictNext24Hours(String city, String target, String startTime);
}
