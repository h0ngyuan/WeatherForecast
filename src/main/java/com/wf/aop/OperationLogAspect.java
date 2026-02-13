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
                String nextAction = state.value(WeatherGraphConstants.KEY_NEXT_ACTION, "");
                String transformedQuestion = state.value(WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, "");
                Object weatherCodeQuery = state.value(WeatherGraphConstants.KEY_WEATHER_CODE_QUERY, null);
                String forecastResult = state.value(WeatherGraphConstants.KEY_FORECAST_RESULT, "");
                String alertCheckResult = state.value(WeatherGraphConstants.KEY_ALERT_CHECK_RESULT, "");
                String generateResult = state.value(WeatherGraphConstants.KEY_GENERATE_RESULT, "");
                Boolean needIntervention = state.value(WeatherGraphConstants.KEY_NEED_INTERVENTION, false);
                Boolean humanFeedback = state.value(WeatherGraphConstants.KEY_HUMAN_FEEDBACK, false);
                List<NodeExecutionRecordEntity> executionRecords = state.value(WeatherGraphConstants.KEY_EXECUTION_RECORDS, List.of());

                UserOperationLogEntity logEntity = new UserOperationLogEntity();
                logEntity.setUserId(1L);
                logEntity.setOperationType("处理过程");
                logEntity.setOperationDesc("天气预测处理流程");
                logEntity.setClientIp(null);
                logEntity.setLocation(null);
                logEntity.setRequestParams(JSON.toJSONString(Map.of("question", question)));
                
                Map<String, Object> resultMap = new java.util.HashMap<>();
                resultMap.put("question", question);
                resultMap.put("answer", answer);
                resultMap.put("relevanceScore", relevanceScore);
                resultMap.put("qualityScore", qualityScore);
                resultMap.put("loopCount", loopCount);
                resultMap.put("nextAction", nextAction);
                resultMap.put("transformedQuestion", transformedQuestion);
                resultMap.put("weatherCodeQuery", weatherCodeQuery);
                resultMap.put("forecastResult", forecastResult);
                resultMap.put("alertCheckResult", alertCheckResult);
                resultMap.put("generateResult", generateResult);
                resultMap.put("needIntervention", needIntervention);
                resultMap.put("humanFeedback", humanFeedback);
                resultMap.put("executionRecords", executionRecords);
                
                logEntity.setResponseResult(JSON.toJSONString(resultMap));

                userOperationLogMapper.insert(logEntity);
                log.info("操作日志记录成功, ID: {}", logEntity.getId());
            }
        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage(), e);
        }
    }
}
