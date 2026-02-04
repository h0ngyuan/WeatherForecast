package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.agent.base.NormalizationResult;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.agent.graph.agent.WeatherNormalizationAgent;
import com.wf.agent.graph.agent.WeatherSemanticTransformAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WeatherSemanticTransformNode implements NodeAction {

    private final WeatherSemanticTransformAgent semanticTransformAgent;
    private final WeatherNormalizationAgent normalizationAgent;

    public WeatherSemanticTransformNode(WeatherSemanticTransformAgent semanticTransformAgent, WeatherNormalizationAgent normalizationAgent) {
        this.semanticTransformAgent = semanticTransformAgent;
        this.normalizationAgent = normalizationAgent;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");

        log.info("开始语义转化流程，原始问题: {}", question);

        String transformedQuestion = semanticTransformAgent.transform(question);
        log.info("语义转化完成: {}", transformedQuestion);

        NormalizationResult result = normalizationAgent.normalize(transformedQuestion);
        log.info("规范化完成: {}", result.getNormalizedQuestion());
        log.info("位置信息: {}", result.getLocationInfo());

        return Map.of(
            WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, result.getNormalizedQuestion(),
            WeatherGraphConstants.KEY_LOCATION_INFO, result.getLocationInfo()
        );
    }
}
