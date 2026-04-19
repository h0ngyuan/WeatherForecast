package com.wf.agent.map.dto;

import lombok.Data;
import java.util.List;

/**
 * 冲突记录
 */
@Data
public class ConflictRecord {
    private String conflictType;
    private String agentA;
    private String agentB;
    private Object valueA;
    private Object valueB;
    private String resolution;
}
