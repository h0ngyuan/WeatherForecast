package com.wf.agent.map.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MapInsightAgent响应
 */
@Data
public class MapInsightResponse {
    private String query;
    private LocalDateTime analysisDate;
    private String conclusion;
    private String explanation;
    private List<String> toolsUsed;
    private List<MapDataPoint> dataPoints;
    private VisualizationSuggestion visualizationSuggestion;
}
