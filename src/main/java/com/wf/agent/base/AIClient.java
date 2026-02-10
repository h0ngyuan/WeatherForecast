package com.wf.agent.base;

import com.wf.agent.constants.WeatherPromptProvider;
import com.wf.agent.tool.LocationTool;
import com.wf.agent.tool.TimeTool;
import com.wf.agent.tool.WeatherCodeTool;
import com.wf.agent.tool.WeatherPredictionTool;
import com.wf.object.entity.NormalizationResult;
import com.wf.object.query.WeatherCodeQuery;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AIClient {

    private final ChatClient chatClient;
    private final WeatherPromptProvider promptProvider;

    public AIClient(ChatClient.Builder chatClientBuilder, WeatherPromptProvider promptProvider) {
        this.chatClient = chatClientBuilder.build();
        this.promptProvider = promptProvider;
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

    public String generateAnswer(String originalQuestion, String normalizedQuestion, String forecastResult) {
        String prompt = promptProvider.getAnswerGenerationPrompt(originalQuestion, normalizedQuestion, forecastResult);
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
        return chatClient.prompt().user(prompt).tools(new TimeTool(), new LocationTool()).call().content();
    }

    public String forecastWeather(String weatherCodeQuery) {
        String prompt = promptProvider.getWeatherForecastPrompt(weatherCodeQuery);
        return chatClient.prompt().user(prompt).tools(new WeatherPredictionTool()).call().content();
    }

    public String performForecast(String weatherCodeQuery) {
        String prompt = promptProvider.getWeatherForecastPrompt(weatherCodeQuery);
        return chatClient.prompt().user(prompt).tools(new WeatherPredictionTool()).call().content();
    }

    public String performForecastTransform(String forecastResult) {
        String prompt = promptProvider.getForecastTransformPrompt(forecastResult);
        return chatClient.prompt().user(prompt).tools(new WeatherCodeTool()).call().content();
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
