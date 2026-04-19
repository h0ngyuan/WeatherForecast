package com.wf.agent.map.memory;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 事件记忆（Redis Hash）
 */
@Data
public class EventMemory {
    private String eventId;
    private String eventType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> affectedCities;
    private Integer maxDisasterLevel;
    private String centerCity;
    private Map<String, Object> metadata;
}
