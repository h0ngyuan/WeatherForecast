package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.object.entity.NormalizationResult;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.agent.constants.WeatherPromptProvider;
import com.wf.agent.tool.LocationTool;
import com.wf.agent.tool.TimeTool;
import com.wf.service.MilvusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

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

    public WeatherSemanticTransformNode(MilvusService milvusService, ChatClient.Builder chatClientBuilder, 
                                        WeatherPromptProvider promptProvider, TimeTool timeTool, LocationTool locationTool) {
        this.milvusService = milvusService;
        this.chatClient = chatClientBuilder.build();
        this.promptProvider = promptProvider;
        this.timeTool = timeTool;
        this.locationTool = locationTool;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("---------- [transform节点] 开始执行 ----------");
        
        String question = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        log.info("原始问题: {}", question);

        log.info("开始语义转化流程...");
        String transformedQuestion = performSemanticTransform(question);
        log.info("语义转化完成: {}", transformedQuestion);

        log.info("开始规范化流程...");
        Map<String, Object> result = performNormalization(transformedQuestion);
        log.info("规范化完成: {}", result.get("normalizedQuestion"));
        log.info("位置信息: {}", result.get("requestInfo"));
        log.info("活动类型: {}", result.get("activityType"));
        log.info("关心条件: {}", result.get("concernCondition"));
        
        log.info("---------- [transform节点] 执行完成 ----------");

        return Map.of(
            WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, result.get("normalizedQuestion"),
            WeatherGraphConstants.KEY_WEATHER_CODE_QUERY, result.get("requestInfo"),
            WeatherGraphConstants.KEY_ACTIVITY_TYPE, result.get("activityType"),
            WeatherGraphConstants.KEY_CONCERN_CONDITION, result.get("concernCondition")
        );
    }

    /**
     * 执行语义转化
     */
    private String performSemanticTransform(String question) {
        List<Map<String, Object>> ragResults = milvusService.milvusSearch(question);
        log.info("RAG检索到 {} 条相关知识", ragResults.size());

        String ragContext = buildRagContext(ragResults);

        String prompt = promptProvider.getSemanticTransformPrompt(question, ragContext);
        String response = chatClient.prompt().user(prompt).call().content();
        log.info("AI转化响应: {}", response);

        return response.trim();
    }

    /**
     * 执行规范化
     */
    private Map<String, Object> performNormalization(String question) {
        String prompt = promptProvider.getCompleteNormalizationPrompt(question);
        String response = chatClient.prompt().user(prompt).tools(timeTool, locationTool).call().content();
        log.info("AI规范化响应: {}", response);

        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(response.trim());
            String normalizedQuestion = json.getString("normalizedQuestion");
            Object requestInfo = json.get("requestInfo");
            String activityType = json.getString("activityType");
            String concernCondition = json.getString("concernCondition");

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("normalizedQuestion", normalizedQuestion);
            result.put("requestInfo", requestInfo);
            result.put("activityType", activityType);
            result.put("concernCondition", concernCondition);
            return result;
        } catch (Exception e) {
            log.error("解析响应失败", e);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("normalizedQuestion", question);
            result.put("requestInfo", null);
            result.put("activityType", null);
            result.put("concernCondition", null);
            return result;
        }
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
