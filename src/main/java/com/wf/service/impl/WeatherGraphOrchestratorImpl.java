package com.wf.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.object.response.WeatherAskResponse;
import com.wf.service.WeatherGraphOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherGraphOrchestratorImpl implements WeatherGraphOrchestrator {

    private final CompiledGraph weatherGraph;

    @Override
    public WeatherAskResponse process(String question) {
        Map<String, Object> initialState = Map.of(
                WeatherGraphConstants.KEY_QUESTION, question,
                WeatherGraphConstants.KEY_LOOP_COUNT, 1
        );

        Optional<OverAllState> result = weatherGraph.invoke(initialState);
        if (result.isEmpty()) {
            return new WeatherAskResponse("系统内部错误", false, 0.0, 0.0, 0);
        }

        OverAllState state = result.get();
        Double relevanceScore = state.value(WeatherGraphConstants.KEY_RELEVANCE_SCORE, 0.0);

        if (relevanceScore < WeatherGraphConstants.THRESHOLD_RELEVANCE) {
            return new WeatherAskResponse("此问题不相干", false, relevanceScore, null, null);
        }

        String answer = state.value(WeatherGraphConstants.KEY_ANSWER, "未能生成有效回答");
        Double qualityScore = state.value(WeatherGraphConstants.KEY_QUALITY_SCORE, 0.0);
        Integer loopCount = state.value(WeatherGraphConstants.KEY_LOOP_COUNT, 1);

        return new WeatherAskResponse(answer, true, relevanceScore, qualityScore, loopCount);
    }
}