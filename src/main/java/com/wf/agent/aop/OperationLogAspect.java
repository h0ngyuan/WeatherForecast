package com.wf.agent.aop;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.wf.agent.state.WeatherState;
import com.wf.mapper.UserOperationLogMapper;
import com.wf.object.entity.UserOperationLogEntity;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final UserOperationLogMapper userOperationLogMapper;

    public OperationLogAspect(UserOperationLogMapper userOperationLogMapper) {
        this.userOperationLogMapper = userOperationLogMapper;
    }

    @Pointcut("execution(* com.wf.agent.graph.node.WeatherAnswerGenerateNode.apply(..))")
    public void answerGenerateNodePointcut() {
    }

    @AfterReturning("answerGenerateNodePointcut()")
    public void saveOperationLog(OverAllState state) {
        try {
            Object weatherStateObj = state.value("weatherState");
            if (weatherStateObj instanceof WeatherState) {
                WeatherState weatherState = (WeatherState) weatherStateObj;

                UserOperationLogEntity logEntity = new UserOperationLogEntity();
                logEntity.setUserId(1L);
                logEntity.setOperationType("处理过程");
                logEntity.setOperationDesc("天气预测处理流程");
                logEntity.setClientIp(null);
                logEntity.setLocation(null);
                logEntity.setRequestParams(weatherState.getQuestion());
                logEntity.setResponseResult(JSON.toJSONString(weatherState.getExecutionRecords()));

                userOperationLogMapper.insert(logEntity);
                log.info("操作日志记录成功, ID: {}", logEntity.getId());
            }
        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage(), e);
        }
    }
}
