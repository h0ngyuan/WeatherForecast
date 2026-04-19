package com.wf.agent.map.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量评测报告
 */
@Data
public class BenchmarkReport {
    private EvaluationReport replayReport;
    private AblationReport ablationReport;
    private long totalExecutionTimeMs;
    private int totalCasesEvaluated;
}
