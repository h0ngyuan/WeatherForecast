package com.wf.agent.map.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 地图天气数据响应
 */
@Data
public class MapWeatherResponse {
    private LocalDate recordDate;
    private Integer cityCount;
    private List<CityWeatherVO> cities;
}
