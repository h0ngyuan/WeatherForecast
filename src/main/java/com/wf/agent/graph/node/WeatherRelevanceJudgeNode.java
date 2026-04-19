package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.agent.base.AIClient;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.object.entity.ChatHistoryEntity;
import com.wf.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WeatherRelevanceJudgeNode implements NodeAction {

    private final AIClient aiClient;
    private final ChatHistoryService chatHistoryService;

    public WeatherRelevanceJudgeNode(AIClient aiClient, ChatHistoryService chatHistoryService) {
        this.aiClient = aiClient;
        this.chatHistoryService = chatHistoryService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("---------- [judge节点] 开始执行 ----------");
        
        String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        log.info("待判断问题: {}", question);
        
        // 获取历史上下文
        java.util.Optional<Object> sessionIdOpt = state.value(WeatherGraphConstants.KEY_SESSION_ID);
        Long sessionId = sessionIdOpt.map(obj -> ((Number) obj).longValue()).orElse(null);
        
        List<ChatHistoryEntity> history = null;
        if (sessionId != null) {
            history = chatHistoryService.getRecentMessages(sessionId, 3); // 获取最近3条
            log.info("获取到 {} 条历史消息", history.size());
        }
        
        log.info("调用AI判断相关性...");
        double score = aiClient.judgeRelevance(question, history);
        log.info("相关性评分: {}", score);
        
        String nextAction = score >= WeatherGraphConstants.THRESHOLD_RELEVANCE ? 
            WeatherGraphConstants.ACTION_NEXT : WeatherGraphConstants.ACTION_END;
        
        String answer = "";
        if (score < WeatherGraphConstants.THRESHOLD_RELEVANCE) {
            answer = "此问题不相干";
            return Map.of(
            WeatherGraphConstants.KEY_RELEVANCE_SCORE, score,
            WeatherGraphConstants.KEY_NEXT_ACTION, nextAction,
            WeatherGraphConstants.KEY_ANSWER, answer
        );
        }
        
        log.info("判断结果: {} (阈值: {})", nextAction, WeatherGraphConstants.THRESHOLD_RELEVANCE);
        log.info("---------- [judge节点] 执行完成 ----------");
        
        return Map.of(
            WeatherGraphConstants.KEY_RELEVANCE_SCORE, score,
            WeatherGraphConstants.KEY_NEXT_ACTION, nextAction
        );
    }
}