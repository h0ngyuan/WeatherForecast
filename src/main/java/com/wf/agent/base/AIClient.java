package com.wf.agent.base;

import com.wf.agent.constants.DisasterPromptProvider;
import com.wf.agent.constants.WeatherPromptProvider;
import com.wf.agent.tool.LocationTool;
import com.wf.agent.tool.MCPPredictionTool;
import com.wf.agent.tool.TimeTool;
import com.wf.agent.tool.WeatherCodeTool;
import com.wf.agent.tool.WeatherPredictionTool;
import com.wf.object.entity.ChatHistoryEntity;
import com.wf.object.entity.NormalizationResult;
import com.wf.object.query.WeatherCodeQuery;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AIClient {

    private final ChatClient chatClient;
    private final WeatherPromptProvider promptProvider;
    private final DisasterPromptProvider disasterPromptProvider;
    private final TimeTool timeTool;
    private final LocationTool locationTool;
    private final WeatherPredictionTool weatherPredictionTool;
    private final WeatherCodeTool weatherCodeTool;
    private final MCPPredictionTool mcpPredictionTool;

    public AIClient(ChatClient.Builder chatClientBuilder, WeatherPromptProvider promptProvider,
                    DisasterPromptProvider disasterPromptProvider,
                    TimeTool timeTool, LocationTool locationTool,
                    WeatherPredictionTool weatherPredictionTool, WeatherCodeTool weatherCodeTool,
                    MCPPredictionTool mcpPredictionTool) {
        this.chatClient = chatClientBuilder.build();
        this.promptProvider = promptProvider;
        this.disasterPromptProvider = disasterPromptProvider;
        this.timeTool = timeTool;
        this.locationTool = locationTool;
        this.weatherPredictionTool = weatherPredictionTool;
        this.weatherCodeTool = weatherCodeTool;
        this.mcpPredictionTool = mcpPredictionTool;
    }

    public ChatClient chatClient() {
        return chatClient;
    }

    public double judgeRelevance(String question, List<ChatHistoryEntity> history) {
        String prompt = promptProvider.getRelevanceJudgePrompt(question, history);
        return parseScore(chatClient.prompt().user(prompt).call().content());
    }

    public String generateAnswer(String question) {
        String prompt = promptProvider.getAnswerGenerationPrompt(question);
        return chatClient.prompt().user(prompt).call().content();
    }

    public String generateAnswer(String originalQuestion, String normalizedQuestion, String forecastResult, String activityType, String concernCondition, String alertCheckResult) {
        String prompt = promptProvider.getFinalGeneratePrompt(originalQuestion, normalizedQuestion, forecastResult, null, alertCheckResult);
        return chatClient.prompt().user(prompt).call().content();
    }

    public double scoreAnswer(String question, String answer) {
        String prompt = promptProvider.getAnswerQualityScorePrompt(question, answer);
        return parseScore(chatClient.prompt().user(prompt).call().content());
    }

    public NormalizationResult normalize(String question) {
        String prompt = promptProvider.getNormalizationPrompt(question);
        String response = chatClient.prompt().user(prompt).call().content();

        return new NormalizationResult(response.trim(), null);
    }

    public String completeNormalize(String question) {
        String prompt = promptProvider.getCompleteNormalizationPrompt(question);
        return chatClient.prompt().user(prompt).tools(timeTool, locationTool).call().content();
    }

    public String forecastWeather(String weatherCodeQuery) {
        String prompt = promptProvider.getWeatherForecastPrompt(weatherCodeQuery);
        return chatClient.prompt().user(prompt).tools(weatherPredictionTool).call().content();
    }

    /**
     * 使用MCP服务获取天气码 - Agent自动决策是否调用MCP
     */
    public String forecastWeatherWithMCPFallback(String location, Double latitude, Double longitude) {
        log.info("使用Agent调用天气服务，location={}, lat={}, lon={}", location, latitude, longitude);
        
        String prompt = String.format("""
            请获取 %s 的天气信息。
            该城市的经纬度为：纬度 %.4f，经度 %.4f。
            
            请按以下步骤执行：
            1. 首先尝试使用本地天气工具获取天气码
            2. 如果本地工具无法获取或返回空结果，则使用 getWeatherFromMCP 工具从MCP服务获取
            3. 返回获取到的天气码列表，格式为逗号分隔的数字
            
            请直接返回天气码，不要有多余解释。
            """, location, latitude, longitude);
        
        // 同时携带本地天气工具和MCP工具，让Agent自动选择
        return chatClient.prompt()
                .user(prompt)
                .tools(weatherPredictionTool, mcpPredictionTool)
                .call()
                .content();
    }

    public String performForecastTransform(String weatherCodes) {
        log.info("Transforming weather codes: {}", weatherCodes);
        String prompt = promptProvider.getForecastTransformPrompt(weatherCodes);
        log.debug("Generated prompt: {}", prompt);
        String result = chatClient.prompt().user(prompt).tools(weatherCodeTool).call().content();
        log.info("Transform result: {}", result);
        return result;
    }

    public String performAlertCheck(String originalQuestion, String forecastResult, String activityType, String concernCondition) {
        String prompt = promptProvider.getAlertCheckPrompt(originalQuestion, forecastResult, activityType, concernCondition);
        return chatClient.prompt().user(prompt).call().content();
    }

    public String performFinalGenerate(String originalQuestion, String normalizedQuestion, String forecastResult, String generateResult, String alertCheckResult) {
        String prompt = promptProvider.getFinalGeneratePrompt(originalQuestion, normalizedQuestion, forecastResult, generateResult, alertCheckResult);
        return chatClient.prompt().user(prompt).call().content();
    }

    public String getWeatherForecastPrompt(String weatherCodeQuery) {
        return promptProvider.getWeatherForecastPrompt(weatherCodeQuery);
    }

    public String getForecastTransformPrompt(String forecastResult) {
        return promptProvider.getForecastTransformPrompt(forecastResult);
    }

    /**
     * 分析灾害 - 紧急响应流程使用
     */
    public String analyzeDisasters(String location, List<Integer> weatherCodes) {
        log.info("[AIClient] 分析 {} 的灾害，天气码: {}", location, weatherCodes);
        String prompt = disasterPromptProvider.getDisasterAnalysisPrompt(location, weatherCodes);
        return chatClient.prompt().user(prompt).call().content();
    }

    private double parseScore(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 构建带历史上下文的对话
     * @param currentQuestion 当前问题
     * @param history 历史聊天记录
     * @return AI回复
     */
    public String chatWithHistory(String currentQuestion, List<ChatHistoryEntity> history) {
        if (history == null || history.isEmpty()) {
            return generateAnswer(currentQuestion);
        }

        // 构建消息列表
        List<Message> messages = new ArrayList<>();

        // 添加系统提示
        String systemPrompt = """
            你是一个天气助手。请根据用户的问题提供天气相关信息。
            注意：如果用户的问题与之前的对话相关，请结合上下文理解用户的意图。
            """;

        // 添加历史消息
        for (ChatHistoryEntity msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        // 添加当前问题
        messages.add(new UserMessage(currentQuestion));

        log.info("使用历史上下文进行对话，历史消息数: {}", history.size());

        return chatClient.prompt()
                .system(systemPrompt)
                .messages(messages)
                .call()
                .content();
    }
}
