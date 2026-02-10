package com.wf.aop;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.object.entity.NodeExecutionRecordEntity;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Aspect
@Component
public class NodeExecutionAspect {

    @Pointcut("@annotation(org.springframework.stereotype.Component) && execution(* com.wf.agent.graph.node.*.apply(..))")
    public void nodeExecutionPointcut() {
    }

    @Around("nodeExecutionPointcut()")
    public Object recordNodeExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String nodeName = joinPoint.getTarget().getClass().getSimpleName();
        log.info("节点开始执行: {}", nodeName);

        long startTime = System.currentTimeMillis();
        Object result = null;
        String output = "";
        Exception exception = null;
        String input = "";

        try {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof OverAllState) {
                OverAllState state = (OverAllState) args[0];
                input = formatInput(state);
            }

            result = joinPoint.proceed();
            output = formatOutput(result);
            log.info("节点执行完成: {}, 耗时: {}ms, 输出: {}", nodeName, System.currentTimeMillis() - startTime, output);
        } catch (Exception e) {
            exception = e;
            output = "执行异常: " + e.getMessage();
            log.error("节点执行异常: {}, 异常信息: {}", nodeName, e.getMessage(), e);
            throw e;
        } finally {
            try {
                Object[] args = joinPoint.getArgs();
                if (args.length > 0 && args[0] instanceof OverAllState) {
                    OverAllState state = (OverAllState) args[0];
                    List<NodeExecutionRecordEntity> executionRecords = state.value(WeatherGraphConstants.KEY_EXECUTION_RECORDS, new ArrayList<>());
                    executionRecords.add(new NodeExecutionRecordEntity(nodeName, input, output));
                }
            } catch (Exception e) {
                log.warn("记录节点执行结果失败: {}", e.getMessage());
            }
        }

        return result;
    }

    private String formatInput(OverAllState state) {
        String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        String transformedQuestion = state.value(WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, "");
        String weatherCodeQuery = state.value(WeatherGraphConstants.KEY_WEATHER_CODE_QUERY, "");
        Integer loopCount = state.value(WeatherGraphConstants.KEY_LOOP_COUNT, 1);

        return String.format("question=%s, transformedQuestion=%s, weatherCodeQuery=%s, loopCount=%d", 
            question, transformedQuestion, weatherCodeQuery, loopCount);
    }

    private String formatOutput(Object result) {
        if (result == null) {
            return "null";
        }
        return result.toString();
    }
}
