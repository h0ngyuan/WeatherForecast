package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.agent.base.WeatherAiService;
import com.wf.agent.constants.WeatherGraphConstants;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WeatherRelevanceJudgeNode implements NodeAction {

    private final WeatherAiService weatherAiService;

    public WeatherRelevanceJudgeNode(WeatherAiService weatherAiService) {
        this.weatherAiService = weatherAiService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        double score = weatherAiService.judgeRelevance(question);
        
        String nextAction = score >= WeatherGraphConstants.THRESHOLD_RELEVANCE ? 
            WeatherGraphConstants.ACTION_NEXT : WeatherGraphConstants.ACTION_END;
        
        return Map.of(
            WeatherGraphConstants.KEY_RELEVANCE_SCORE, score,
            WeatherGraphConstants.KEY_NEXT_ACTION, nextAction
        );
    }
}