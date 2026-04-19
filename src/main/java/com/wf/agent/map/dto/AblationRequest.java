package com.wf.agent.map.dto;

import lombok.Data;
import java.util.Set;

/**
 * 消融测试请求
 */
@Data
public class AblationRequest {
    private String location;
    private Integer radiusKm;
    private String date;
    private Set<String> excludeAgents;
}
