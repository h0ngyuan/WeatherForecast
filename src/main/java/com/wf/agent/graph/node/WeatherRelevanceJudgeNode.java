package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.agent.base.AiScoringService;
import com.wf.agent.constants.WeatherGraphConstants;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WeatherRelevanceJudgeNode implements NodeAction {

    private final AiScoringService aiScoringService;

    public WeatherRelevanceJudgeNode(AiScoringService aiScoringService) {
        this.aiScoringService = aiScoringService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        double score = aiScoringService.judgeRelevance(question);
        return Map.of(WeatherGraphConstants.KEY_RELEVANCE_SCORE, score);
    }
}