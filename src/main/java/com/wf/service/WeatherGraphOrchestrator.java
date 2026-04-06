package com.wf.service;

import com.wf.object.request.WeatherPermissionRequest;
import com.wf.object.response.WeatherAskResponse;

public interface WeatherGraphOrchestrator {
    WeatherAskResponse process(String question);

    WeatherAskResponse processWithThread(String question, Long userId);

    void grantPermission(String threadId, Long userId, WeatherPermissionRequest request);

    WeatherAskResponse resume(String threadId, Long userId);

    WeatherAskResponse rejectPermission(String threadId, Long userId);
}
