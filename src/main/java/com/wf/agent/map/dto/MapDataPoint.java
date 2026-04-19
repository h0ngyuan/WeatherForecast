package com.wf.agent.map.dto;

import lombok.Data;

/**
 * 地图数据点
 */
@Data
public class MapDataPoint {
    private String cityCode;
    private String cityName;
    private double latitude;
    private double longitude;
    private String dataType;
    private Object value;
    private String description;
}
