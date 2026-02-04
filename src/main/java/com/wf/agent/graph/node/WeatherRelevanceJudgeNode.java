package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.agent.base.AIClient;
import com.wf.agent.constants.WeatherGraphConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WeatherRelevanceJudgeNode implements NodeAction {

    private final AIClient aiClient;

    public WeatherRelevanceJudgeNode(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("---------- [judge节点] 开始执行 ----------");
        
        String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        log.info("待判断问题: {}", question);
        
        log.info("调用AI判断相关性...");
        double score = aiClient.judgeRelevance(question);
        log.info("相关性评分: {}", score);
        
        String nextAction = score >= WeatherGraphConstants.THRESHOLD_RELEVANCE ? 
            WeatherGraphConstants.ACTION_NEXT : WeatherGraphConstants.ACTION_END;
        
        log.info("判断结果: {} (阈值: {})", nextAction, WeatherGraphConstants.THRESHOLD_RELEVANCE);
        log.info("---------- [judge节点] 执行完成 ----------");
        
        return Map.of(
            WeatherGraphConstants.KEY_RELEVANCE_SCORE, score,
            WeatherGraphConstants.KEY_NEXT_ACTION, nextAction
        );
    }
}