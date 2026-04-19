package com.wf.agent.map.service;

import com.wf.agent.map.dto.MapInsightRequest;
import com.wf.agent.map.dto.MapInsightResponse;
import com.wf.agent.map.dto.MapWeatherResponse;

import java.time.LocalDate;

/**
 * 地图服务接口
 */
public interface MapService {

    /**
     * 获取地图天气数据
     */
    MapWeatherResponse getMapWeatherData(LocalDate date);

    /**
     * MapInsightAgent分析
     */
    MapInsightResponse analyze(MapInsightRequest request);
}
