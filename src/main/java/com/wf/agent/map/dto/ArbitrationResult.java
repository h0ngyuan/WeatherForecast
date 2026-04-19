package com.wf.agent.map.dto;

import lombok.Data;
import java.util.List;

/**
 * 仲裁结果
 */
@Data
public class ArbitrationResult {
    private int finalRiskLevel;
    private double finalConfidence;
    private String arbitrationReason;
    private List<AgentReport> agentReports;
    private List<ConflictRecord> conflicts;
}
