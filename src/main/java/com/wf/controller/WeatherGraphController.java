package com.wf.controller;

import com.wf.object.request.WeatherAskRequest;
import com.wf.object.response.WeatherAskResponse;
import com.wf.service.WeatherGraphOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherGraphController {

    private final WeatherGraphOrchestrator orchestrator;

    @PostMapping("/query")
    public ResponseEntity<WeatherAskResponse> ask(@Valid @RequestBody WeatherAskRequest request) {
        try {
            WeatherAskResponse response = orchestrator.process(request.question());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new WeatherAskResponse("服务暂时不可用", false, 0.0, 0.0, 0));
        }
    }
}