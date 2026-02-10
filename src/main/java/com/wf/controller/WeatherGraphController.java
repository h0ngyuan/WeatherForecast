package com.wf.controller;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import com.wf.object.request.WeatherAskRequest;
import com.wf.object.response.WeatherAskResponse;
import com.wf.service.WeatherDataService;
import com.wf.service.WeatherGraphOrchestrator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherGraphController {

    private final WeatherGraphOrchestrator orchestrator;
    private final WeatherDataService weatherDataService;

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

    @Schema(description = "测试——获取历史范围内天气数据")
    @PostMapping("/fetch-data")
    public R fetchWeatherData(@RequestParam("beginTime")Integer beginTime,@RequestParam("endTime")Integer endTime) {
        try {
            weatherDataService.manualFetchWeatherData(beginTime,endTime);
            return R.success("天气数据获取成功");
        } catch (Exception e) {
            log.error("手动获取天气数据失败", e);
            return R.fail("天气数据获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/predict")
    public R predictWeatherData() {
        try {
            log.info("手动触发天气预测任务");
            weatherDataService.predictWeatherData();
            return R.success("天气预测任务执行成功");
        } catch (Exception e) {
            log.error("手动触发天气预测失败", e);
            return R.fail("天气预测任务失败: " + e.getMessage());
        }
    }
}