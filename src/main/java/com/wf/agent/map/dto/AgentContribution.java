package com.wf.agent.map.dto;

import lombok.Data;

/**
 * Agent贡献度
 */
@Data
public class AgentContribution {
    private String agentName;
    private int riskLevelImpact;
    private double confidenceImpact;
    private String description;
}
