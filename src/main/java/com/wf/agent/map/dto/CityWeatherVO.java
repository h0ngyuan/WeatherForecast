package com.wf.agent.map.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 城市天气VO
 */
@Data
public class CityWeatherVO {
    private String cityCode;
    private String cityName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String province;
    private Integer cityLevel;
    
    private List<Integer> hourlyWeatherCodes;
    private Integer dayWeatherCode;
    private Boolean hasDisaster;
    private String disasterTypes;
    private Integer maxDisasterLevel;
    
    private String displayColor;
    private String displayIcon;
}
