package com.wf.agent.map.memory;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 建议项（Redis List Item）
 */
@Data
public class Suggestion {
    private String suggestionId;
    private String agentName;
    private String type;
    private String content;
    private String reason;
    private Integer priority;
    private Double confidence;
    private LocalDateTime timestamp;
    private String status;
}
