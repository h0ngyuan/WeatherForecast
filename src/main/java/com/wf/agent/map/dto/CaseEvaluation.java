package com.wf.agent.map.dto;

import lombok.Data;

/**
 * 案例评估详情
 */
@Data
public class CaseEvaluation {
    private String caseId;
    private String eventType;
    private int expectedRiskLevel;
    private int actualRiskLevel;
    private boolean matches;
    private int leadTimeMinutes;
    private long responseDelayMs;
}
