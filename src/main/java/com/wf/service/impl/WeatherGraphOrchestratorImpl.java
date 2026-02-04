package com.wf.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.object.response.WeatherAskResponse;
import com.wf.service.WeatherGraphOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherGraphOrchestratorImpl implements WeatherGraphOrchestrator {

    private final CompiledGraph weatherGraph;

    @Override
    public WeatherAskResponse process(String question) {
        log.info("========== WeatherGraph 开始处理 ==========");
        log.info("用户问题: {}", question);

        Map<String, Object> initialState = Map.of(
                WeatherGraphConstants.KEY_QUESTION, question,
                WeatherGraphConstants.KEY_LOOP_COUNT, 1
        );

        log.info("初始化状态完成，开始调用 graph");
        Optional<OverAllState> result = weatherGraph.invoke(initialState);
        
        if (result.isEmpty()) {
            log.error("Graph 执行失败，返回空结果");
            return new WeatherAskResponse("系统内部错误", false, 0.0, 0.0, 0);
        }

        OverAllState state = result.get();
        Double relevanceScore = state.value(WeatherGraphConstants.KEY_RELEVANCE_SCORE, 0.0);
        log.info("相关性评分: {}", relevanceScore);

        if (relevanceScore < WeatherGraphConstants.THRESHOLD_RELEVANCE) {
            log.warn("问题相关性不足，阈值: {}, 实际: {}", WeatherGraphConstants.THRESHOLD_RELEVANCE, relevanceScore);
            return new WeatherAskResponse("此问题不相干", false, relevanceScore, null, null);
        }

        String answer = state.value(WeatherGraphConstants.KEY_ANSWER, "未能生成有效回答");
        Double qualityScore = state.value(WeatherGraphConstants.KEY_QUALITY_SCORE, 0.0);
        Integer loopCount = state.value(WeatherGraphConstants.KEY_LOOP_COUNT, 1);

        log.info("生成答案成功");
        log.info("质量评分: {}, 循环次数: {}", qualityScore, loopCount);
        log.info("最终答案: {}", answer);
        log.info("========== WeatherGraph 处理完成 ==========");

        return new WeatherAskResponse(answer, true, relevanceScore, qualityScore, loopCount);
    }
}