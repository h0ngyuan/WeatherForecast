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
public class WeatherAnswerGenerateNode implements NodeAction {

    private final AIClient aiClient;

    public WeatherAnswerGenerateNode(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("---------- [generate节点] 开始执行 ----------");
        
        String originalQuestion = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        String normalizedQuestion = state.value(WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, "");
        String locationInfo = state.value(WeatherGraphConstants.KEY_LOCATION_INFO, "");
        Integer loopCount = state.value(WeatherGraphConstants.KEY_LOOP_COUNT, 1);
        
        log.info("当前循环次数: {}", loopCount);
        log.info("原始问题: {}", originalQuestion);
        log.info("规范化问题: {}", normalizedQuestion);
        log.info("位置信息: {}", locationInfo);

        log.info("调用AI生成答案...");
        String answer;
        if (normalizedQuestion != null && !normalizedQuestion.isEmpty()) {
            answer = aiClient.generateAnswer(originalQuestion, normalizedQuestion, locationInfo);
        } else {
            answer = aiClient.generateAnswer(originalQuestion);
        }
        log.info("生成答案: {}", answer);
        
        log.info("调用AI评分答案质量...");
        double qualityScore = aiClient.scoreAnswer(originalQuestion, answer);
        log.info("质量评分: {}", qualityScore);

        loopCount++;
        String nextAction = (qualityScore >= WeatherGraphConstants.THRESHOLD_QUALITY ||
                loopCount > WeatherGraphConstants.MAX_LOOP_COUNT) ? "break" : "loop";
        
        log.info("质量阈值: {}, 最大循环次数: {}", WeatherGraphConstants.THRESHOLD_QUALITY, WeatherGraphConstants.MAX_LOOP_COUNT);
        log.info("下一步操作: {}", nextAction);
        log.info("---------- [generate节点] 执行完成 ----------");

        return Map.of(
                WeatherGraphConstants.KEY_ANSWER, answer,
                WeatherGraphConstants.KEY_QUALITY_SCORE, qualityScore,
                WeatherGraphConstants.KEY_LOOP_COUNT, loopCount,
                WeatherGraphConstants.KEY_NEXT_ACTION, nextAction
        );
    }
}