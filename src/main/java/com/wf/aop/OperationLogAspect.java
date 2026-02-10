package com.wf.aop;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.mapper.UserOperationLogMapper;
import com.wf.object.entity.NodeExecutionRecordEntity;
import com.wf.object.entity.UserOperationLogEntity;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final UserOperationLogMapper userOperationLogMapper;

    public OperationLogAspect(UserOperationLogMapper userOperationLogMapper) {
        this.userOperationLogMapper = userOperationLogMapper;
    }

    @Pointcut("execution(* com.alibaba.cloud.ai.graph.CompiledGraph.invoke(..))")
    public void compiledGraphInvokePointcut() {
    }

    @AfterReturning(value = "compiledGraphInvokePointcut()", returning = "result")
    public void saveOperationLog(JoinPoint joinPoint, Optional<OverAllState> result) {
        try {
            if (result.isPresent()) {
                OverAllState state = result.get();
                String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");
                String answer = state.value(WeatherGraphConstants.KEY_ANSWER, "");
                Double relevanceScore = state.value(WeatherGraphConstants.KEY_RELEVANCE_SCORE, 0.0);
                Double qualityScore = state.value(WeatherGraphConstants.KEY_QUALITY_SCORE, 0.0);
                Integer loopCount = state.value(WeatherGraphConstants.KEY_LOOP_COUNT, 1);
                List<NodeExecutionRecordEntity> executionRecords = state.value(WeatherGraphConstants.KEY_EXECUTION_RECORDS, List.of());

                UserOperationLogEntity logEntity = new UserOperationLogEntity();
                logEntity.setUserId(1L);
                logEntity.setOperationType("处理过程");
                logEntity.setOperationDesc("天气预测处理流程");
                logEntity.setClientIp(null);
                logEntity.setLocation(null);
                logEntity.setRequestParams(question);
                logEntity.setResponseResult(JSON.toJSONString(Map.of(
                    "answer", answer,
                    "relevanceScore", relevanceScore,
                    "qualityScore", qualityScore,
                    "loopCount", loopCount
                )));

                userOperationLogMapper.insert(logEntity);
                log.info("操作日志记录成功, ID: {}", logEntity.getId());
            }
        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage(), e);
        }
    }
}
