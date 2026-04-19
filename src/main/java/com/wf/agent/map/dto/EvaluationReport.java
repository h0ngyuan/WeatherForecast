package com.wf.agent.map.dto;

import lombok.Data;
import java.util.List;

/**
 * 评测报告
 */
@Data
public class EvaluationReport {
    private int totalCases;
    private double accuracy;
    private double avgLeadTimeMinutes;
    private double consistency;
    private long avgResponseDelayMs;
    private List<CaseEvaluation> caseDetails;
}
