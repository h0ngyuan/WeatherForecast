package com.wf.agent.map.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 历史案例实体
 */
@Data
@TableName("HISTORICAL_CASE")
public class HistoricalCase {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String caseId;
    private String eventType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private String inputData;
    private String decisionChain;
    private String finalResult;
    
    private BigDecimal accuracyScore;
    private Integer leadTimeMinutes;
    private BigDecimal consistencyScore;
    private Integer responseDelayMs;
    
    private String agentConfigVersion;
    
    private LocalDateTime createTime;
}
