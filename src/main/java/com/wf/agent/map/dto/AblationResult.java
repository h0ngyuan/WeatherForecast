package com.wf.agent.map.dto;

import lombok.Data;

/**
 * 消融结果
 */
@Data
public class AblationResult {
    private int riskLevel;
    private double confidence;
    private String conclusion;
}
