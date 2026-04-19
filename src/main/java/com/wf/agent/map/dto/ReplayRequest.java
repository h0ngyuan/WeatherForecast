package com.wf.agent.map.dto;

import lombok.Data;

/**
 * 历史案例回放请求
 */
@Data
public class ReplayRequest {
    private String caseId;
    private String location;
}
