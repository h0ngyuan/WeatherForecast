package com.wf.agent.map.controller;

import com.wf.agent.map.dto.MapInsightRequest;
import com.wf.agent.map.dto.MapInsightResponse;
import com.wf.agent.map.dto.MapWeatherResponse;
import com.wf.agent.map.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 地图模块控制器
 */
@RestController
@RequestMapping("/api/v1/map")
@Slf4j
public class MapController {

    @Autowired
    private MapService mapService;

    /**
     * 获取地图天气数据
     */
    @GetMapping("/weather-data")
    public R<MapWeatherResponse> getMapWeatherData(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("[MapController] 获取地图天气数据, date={}", date);
        MapWeatherResponse response = mapService.getMapWeatherData(date);
        return R.data(response);
    }

    /**
     * MapInsightAgent分析接口
     */
    @PostMapping("/analyze")
    public R<MapInsightResponse> analyze(@RequestBody MapInsightRequest request) {
        log.info("[MapController] MapInsightAgent分析: {}", request.getQuery());
        MapInsightResponse response = mapService.analyze(request);
        return R.data(response);
    }

    /**
     * 快速查询：周边灾害分析
     */
    @GetMapping("/nearby-analysis")
    public R<MapInsightResponse> analyzeNearby(
            @RequestParam String cityCode,
            @RequestParam(defaultValue = "100") int radiusKm) {
        log.info("[MapController] 周边分析: city={}, radius={}km", cityCode, radiusKm);

        MapInsightRequest request = new MapInsightRequest();
        request.setQuery("分析" + cityCode + "周边" + radiusKm + "公里内的灾害分布");
        request.setCityCode(cityCode);

        MapInsightResponse response = mapService.analyze(request);
        return R.data(response);
    }
}
