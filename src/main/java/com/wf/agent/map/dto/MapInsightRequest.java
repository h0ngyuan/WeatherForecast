package com.wf.agent.map.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * MapInsightAgent请求
 */
@Data
public class MapInsightRequest {
    private String query;
    private LocalDate date;
    private String region;
    private String cityCode;
    private String eventId;
    private Map<String, Object> context;
}
