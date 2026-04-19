package com.wf.agent.map.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Agent报告标准格式 - 所有SubAgent的输出格式
 */
@Data
public class AgentReport {
    private String agentName;
    private String reportType;
    private String conclusion;
    private int riskLevel;
    private double confidence;
    private List<String> evidence;
    private Map<String, Object> details;
    private long executionTimeMs;
}
