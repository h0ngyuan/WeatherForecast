package com.wf.agent.map.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 城市天气日记录实体
 */
@Data
@TableName("CITY_WEATHER_DAILY")
public class CityWeatherDaily {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String cityCode;
    private String cityName;
    private LocalDate recordDate;
    
    private String hourlyWeatherCodes;
    private Integer dayWeatherCode;
    private Integer nightWeatherCode;
    private BigDecimal maxTemp;
    private BigDecimal minTemp;
    
    private Integer hasDisaster;
    private String disasterTypes;
    private Integer maxDisasterLevel;
    
    private String predictedRisks;
    
    private Integer isAnomaly;
    private BigDecimal anomalyScore;
    private String anomalyReason;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
