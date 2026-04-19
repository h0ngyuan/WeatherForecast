package com.wf.agent.map.dto;

import lombok.Data;

import java.util.List;

/**
 * 可视化建议
 */
@Data
public class VisualizationSuggestion {
    private String title;
    private String description;
    private String colorScheme;
    private List<String> highlightCities;
    private boolean showCluster;
    private List<String> overlayLayers;
}
