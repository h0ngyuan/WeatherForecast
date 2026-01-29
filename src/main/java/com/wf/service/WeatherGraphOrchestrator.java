package com.wf.service;

import com.wf.object.response.WeatherAskResponse;

public interface WeatherGraphOrchestrator {
    WeatherAskResponse process(String question);
}