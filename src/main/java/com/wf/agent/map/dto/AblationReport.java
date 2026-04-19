package com.wf.agent.map.dto;

import lombok.Data;
import java.util.Set;

/**
 * 消融报告
 */
@Data
public class AblationReport {
    private AblationResult fullConfig;
    private AblationResult withoutTrend;
    private AblationResult withoutSeason;
    private AblationResult withoutImpact;
    private AgentContribution trendContribution;
    private AgentContribution seasonContribution;
    private AgentContribution impactContribution;
    private String mostCriticalAgent;
}
