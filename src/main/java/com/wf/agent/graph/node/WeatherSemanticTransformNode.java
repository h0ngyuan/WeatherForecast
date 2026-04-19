package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.agent.constants.WeatherPromptProvider;
import com.wf.agent.tool.LocationTool;
import com.wf.agent.tool.TimeTool;
import com.wf.object.entity.ChatHistoryEntity;
import com.wf.service.ChatHistoryService;
import com.wf.service.MilvusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WeatherSemanticTransformNode implements NodeAction {

    private final MilvusService milvusService;
    private final ChatClient chatClient;
    private final WeatherPromptProvider promptProvider;
    private final TimeTool timeTool;
    private final LocationTool locationTool;
    private final ChatHistoryService chatHistoryService;

    public WeatherSemanticTransformNode(MilvusService milvusService, ChatClient.Builder chatClientBuilder, 
                                        WeatherPromptProvider promptProvider, TimeTool timeTool, 
                                        LocationTool locationTool, ChatHistoryService chatHistoryService) {
        this.milvusService = milvusService;
        this.chatClient = chatClientBuilder.build();
        this.promptProvider = promptProvider;
        this.timeTool = timeTool;
        this.locationTool = locationTool;
        this.chatHistoryService = chatHistoryService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("---------- [transform节点] 开始执行 ----------");
        
        String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        java.util.Optional<Object> sessionIdOpt = state.value(WeatherGraphConstants.KEY_SESSION_ID);
        Long sessionId = sessionIdOpt.map(obj -> ((Number) obj).longValue()).orElse(null);
        log.info("原始问题: {}, 会话ID: {}", question, sessionId);

        // 获取历史上下文
        List<ChatHistoryEntity> history = null;
        if (sessionId != null) {
            history = chatHistoryService.getRecentMessages(sessionId, 5); // 获取最近5条
            log.info("获取到 {} 条历史消息", Arrays.toString(history.toArray()));
        }

        log.info("开始语义转化流程...");
        String transformedQuestion = performSemanticTransform(question, history);
        log.info("语义转化完成: {}", transformedQuestion);

        log.info("开始规范化流程...");
        Map<String, String> result = performNormalization(transformedQuestion, history);
        log.info("规范化完成: {}", result.get("normalizedQuestion"));
        log.info("位置信息: {}", result.get("requestInfo"));
        log.info("活动类型: {}", result.get("activityType"));
        log.info("关心条件: {}", result.get("concernCondition"));
        
        log.info("准备返回结果到状态...");
        Map<String, Object> returnResult = Map.of(
            WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, result.get("normalizedQuestion")==null?"":result.get("normalizedQuestion"),
            WeatherGraphConstants.KEY_WEATHER_CODE_QUERY, result.get("requestInfo")==null?"":result.get("requestInfo"),
            WeatherGraphConstants.KEY_ACTIVITY_TYPE, result.get("activityType")==null?"":result.get("activityType"),
            WeatherGraphConstants.KEY_CONCERN_CONDITION, result.get("concernCondition")==null?"":result.get("concernCondition")
        );
        log.info("返回结果: {}", returnResult);
        log.info("---------- [transform节点] 执行完成 ----------");

        return returnResult;
    }

    /**
     * 执行语义转化
     */
    private String performSemanticTransform(String question, List<ChatHistoryEntity> history) {
        List<Map<String, Object>> ragResults = milvusService.milvusSearch(question);
        log.info("RAG检索到 {} 条相关知识", ragResults.size());

        String ragContext = buildRagContext(ragResults);

        // 构建带历史上下文的提示词
        String prompt = buildPromptWithHistory(
            promptProvider.getSemanticTransformPrompt(question, ragContext),
            history
        );
        String response = chatClient.prompt().user(prompt).call().content();
        log.info("AI转化响应: {}", response);

        return response.trim();
    }

    /**
     * 执行规范化
     */
    private Map<String, String> performNormalization(String question, List<ChatHistoryEntity> history) {
        // 构建带历史上下文的提示词
        String prompt = buildPromptWithHistory(
            promptProvider.getCompleteNormalizationPrompt(question),
            history
        );
        String response = chatClient.prompt().user(prompt).tools(timeTool, locationTool).call().content();
        log.info("AI规范化响应: {}", response);

        try {
            // 提取 JSON 内容（处理 Markdown 代码块）
            String jsonStr = extractJsonFromResponse(response.trim());
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(jsonStr);
            String normalizedQuestion = json.getString("normalizedQuestion");
            Object requestInfoObj = json.get("requestInfo");
            String requestInfo = requestInfoObj != null ? requestInfoObj.toString() : null;
            String activityType = json.getString("activityType");
            String concernCondition = json.getString("concernCondition");

            log.info("解析requestInfo: {}", requestInfo);

            Map<String,String> result = new java.util.HashMap<>();
            result.put("normalizedQuestion", normalizedQuestion);
            result.put("requestInfo", requestInfo);
            result.put("activityType", activityType);
            result.put("concernCondition", concernCondition);
            return result;
        } catch (Exception e) {
            log.error("解析响应失败", e);
            Map<String, String> result = new java.util.HashMap<>();
            result.put("normalizedQuestion", question);
            result.put("requestInfo", null);
            result.put("activityType", null);
            result.put("concernCondition", null);
            return result;
        }
    }

    /**
     * 从 AI 响应中提取 JSON 内容
     * 处理 Markdown 代码块和额外说明文字
     */
    private String extractJsonFromResponse(String response) {
        // 尝试提取 ```json ... ``` 或 ``` ... ``` 中的内容
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        // 如果没有代码块，尝试直接找到 JSON 对象
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }
        return response;
    }

    /**
     * 构建带历史上下文的提示词
     */
    private String buildPromptWithHistory(String originalPrompt, List<ChatHistoryEntity> history) {
        if (history == null || history.isEmpty()) {
            return originalPrompt;
        }

        StringBuilder historyContext = new StringBuilder();
        historyContext.append("\n\n【历史对话上下文】\n");
        for (ChatHistoryEntity msg : history) {
            if ("user".equals(msg.getRole())) {
                historyContext.append("用户: ").append(msg.getContent()).append("\n");
            } else if ("assistant".equals(msg.getRole())) {
                historyContext.append("助手: ").append(msg.getContent()).append("\n");
            }
        }
        historyContext.append("\n请结合以上历史对话理解当前问题。\n");

        return originalPrompt + historyContext.toString();
    }

    /**
     * 构建RAG上下文
     */
    private String buildRagContext(List<Map<String, Object>> ragResults) {
        if (ragResults.isEmpty()) {
            return "未检索到相关知识";
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < ragResults.size(); i++) {
            Map<String, Object> result = ragResults.get(i);
            context.append(i + 1).append(". ")
                   .append(result.get("content"))
                   .append(" (分类: ")
                   .append(result.get("category"))
                   .append(")\n");
        }
        log.info(context.toString());
        return context.toString();
    }
}
