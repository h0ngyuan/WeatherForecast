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
public class WeatherFinalGenerateNode implements NodeAction {

    private final AIClient aiClient;

    public WeatherFinalGenerateNode(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("---------- [finalGenerate节点] 开始执行 ----------");

        String originalQuestion = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        String normalizedQuestion = state.value(WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, "");
        String forecastResult = state.value(WeatherGraphConstants.KEY_FORECAST_RESULT, "");
        String generateResult = state.value(WeatherGraphConstants.KEY_GENERATE_RESULT, "");
        String alertCheckResult = state.value(WeatherGraphConstants.KEY_ALERT_CHECK_RESULT, "");
        Integer loopCount = state.value(WeatherGraphConstants.KEY_LOOP_COUNT, 1);

        log.info("当前循环次数: {}", loopCount);
        log.info("原始问题: {}", originalQuestion);
        log.info("规范化问题: {}", normalizedQuestion);
        log.info("预测结果: {}", forecastResult);
        log.info("初步生成答案: {}", generateResult);
        log.info("预警检查结果: {}", alertCheckResult);

        log.info("调用AI生成最终答案...");
        String finalAnswer = aiClient.performFinalGenerate(originalQuestion, normalizedQuestion, forecastResult, generateResult, alertCheckResult);
        log.info("生成最终答案: {}", finalAnswer);

        log.info("调用AI评分答案质量...");
        double qualityScore = aiClient.scoreAnswer(originalQuestion, finalAnswer);
        log.info("质量评分: {}", qualityScore);

        loopCount++;
        String nextAction = (qualityScore >= WeatherGraphConstants.THRESHOLD_QUALITY ||
                loopCount > WeatherGraphConstants.MAX_LOOP_COUNT) ? "break" : "loop";

        log.info("质量阈值: {}, 最大循环次数: {}", WeatherGraphConstants.THRESHOLD_QUALITY, WeatherGraphConstants.MAX_LOOP_COUNT);
        log.info("下一步操作: {}", nextAction);
        log.info("---------- [finalGenerate节点] 执行完成 ----------");

        return Map.of(
            WeatherGraphConstants.KEY_ANSWER, finalAnswer,
            WeatherGraphConstants.KEY_QUALITY_SCORE, qualityScore,
            WeatherGraphConstants.KEY_LOOP_COUNT, loopCount,
            WeatherGraphConstants.KEY_NEXT_ACTION, nextAction
        );
    }
}
