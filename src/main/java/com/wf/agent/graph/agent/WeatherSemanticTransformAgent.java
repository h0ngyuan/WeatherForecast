package com.wf.agent.graph.agent;

import com.wf.agent.constants.WeatherPromptProvider;
import com.wf.service.MilvusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WeatherSemanticTransformAgent {

    private final MilvusService milvusService;
    private final ChatClient chatClient;
    private final WeatherPromptProvider promptProvider;

    public WeatherSemanticTransformAgent(MilvusService milvusService, ChatClient.Builder chatClientBuilder, WeatherPromptProvider promptProvider) {
        this.milvusService = milvusService;
        this.chatClient = chatClientBuilder.build();
        this.promptProvider = promptProvider;
    }

    public String transform(String question) {
        log.info("开始语义转化，原始问题: {}", question);

        List<Map<String, Object>> ragResults = milvusService.milvusSearch(question);
        log.info("RAG检索到 {} 条相关知识", ragResults.size());

        String ragContext = buildRagContext(ragResults);

        String prompt = promptProvider.getSemanticTransformPrompt(question, ragContext);
        String response = chatClient.prompt().user(prompt).call().content();
        log.info("AI转化响应: {}", response);

        return response.trim();
    }

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
        return context.toString();
    }
}
