package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.agent.base.WeatherAiService;
import com.wf.agent.constants.WeatherGraphConstants;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WeatherAnswerGenerateNode implements NodeAction {

    private final WeatherAiService weatherAiService;

    public WeatherAnswerGenerateNode(WeatherAiService weatherAiService) {
        this.weatherAiService = weatherAiService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        Integer loopCount = state.value(WeatherGraphConstants.KEY_LOOP_COUNT, 1);

        String answer = weatherAiService.generateAnswer(question);
        double qualityScore = weatherAiService.scoreAnswer(question, answer);

        loopCount++;
        String nextAction = (qualityScore >= WeatherGraphConstants.THRESHOLD_QUALITY ||
                loopCount > WeatherGraphConstants.MAX_LOOP_COUNT) ? "break" : "loop";

        return Map.of(
                WeatherGraphConstants.KEY_ANSWER, answer,
                WeatherGraphConstants.KEY_QUALITY_SCORE, qualityScore,
                WeatherGraphConstants.KEY_LOOP_COUNT, loopCount,
                WeatherGraphConstants.KEY_NEXT_ACTION, nextAction
        );
    }
}