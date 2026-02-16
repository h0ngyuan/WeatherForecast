package com.wf.agent.base;

import com.wf.agent.constants.WeatherPromptProvider;
import com.wf.agent.tool.LocationTool;
import com.wf.agent.tool.MCPPredictionTool;
import com.wf.agent.tool.TimeTool;
import com.wf.agent.tool.WeatherCodeTool;
import com.wf.agent.tool.WeatherPredictionTool;
import com.wf.object.entity.NormalizationResult;
import com.wf.object.query.WeatherCodeQuery;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AIClient {

    private final ChatClient chatClient;
    private final WeatherPromptProvider promptProvider;
    private final TimeTool timeTool;
    private final LocationTool locationTool;
    private final WeatherPredictionTool weatherPredictionTool;
    private final WeatherCodeTool weatherCodeTool;
    private final MCPPredictionTool mcpPredictionTool;

    public AIClient(ChatClient.Builder chatClientBuilder, WeatherPromptProvider promptProvider,
                    TimeTool timeTool, LocationTool locationTool,
                    WeatherPredictionTool weatherPredictionTool, WeatherCodeTool weatherCodeTool,
                    MCPPredictionTool mcpPredictionTool) {
        this.chatClient = chatClientBuilder.build();
        this.promptProvider = promptProvider;
        this.timeTool = timeTool;
        this.locationTool = locationTool;
        this.weatherPredictionTool = weatherPredictionTool;
        this.weatherCodeTool = weatherCodeTool;
        this.mcpPredictionTool = mcpPredictionTool;
    }

    public ChatClient chatClient() {
        return chatClient;
    }

    public double judgeRelevance(String question) {
        String prompt = promptProvider.getRelevanceJudgePrompt(question);
        return parseScore(chatClient.prompt().user(prompt).call().content());
    }

    public String generateAnswer(String question) {
        String prompt = promptProvider.getAnswerGenerationPrompt(question);
        return chatClient.prompt().user(prompt).call().content();
    }

    public String generateAnswer(String originalQuestion, String normalizedQuestion, String forecastResult, String activityType, String concernCondition) {
        String prompt = promptProvider.getAnswerGenerationPrompt(originalQuestion, normalizedQuestion, forecastResult, activityType, concernCondition);
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

    private double parseScore(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
