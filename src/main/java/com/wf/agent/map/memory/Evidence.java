package com.wf.agent.map.memory;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 证据项（Redis List Item）
 */
@Data
public class Evidence {
    private String evidenceId;
    private String agentName;
    private String evidenceType;
    private String content;
    private Double confidence;
    private List<String> dataSources;
    private LocalDateTime timestamp;
    private String relatedRule;
}
