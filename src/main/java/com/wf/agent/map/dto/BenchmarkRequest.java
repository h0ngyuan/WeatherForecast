package com.wf.agent.map.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量评测请求
 */
@Data
public class BenchmarkRequest {
    private String location;
    private Integer caseLimit;
    private boolean runAblation;
}
