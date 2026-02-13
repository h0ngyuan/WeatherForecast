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

    public AIClient(ChatClient.Builder chatClientBuilder, WeatherPromptProvider promptProvider,
                    TimeTool timeTool, LocationTool locationTool,
                    WeatherPredictionTool weatherPredictionTool, WeatherCodeTool weatherCodeTool) {
        this.chatClient = chatClientBuilder.build();
        this.promptProvider = promptProvider;
        this.timeTool = timeTool;
        this.locationTool = locationTool;
        this.weatherPredictionTool = weatherPredictionTool;
        this.weatherCodeTool = weatherCodeTool;
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
        return chatClient.prompt().user(prompt).tools(timeTool, locationTool).call().content();
    }

    public String forecastWeather(String weatherCodeQuery) {
        String prompt = promptProvider.getWeatherForecastPrompt(weatherCodeQuery);
        return chatClient.prompt().user(prompt).tools(weatherPredictionTool).call().content();
    }

    public String performForecast(WeatherCodeQuery query) {
        log.info("Performing forecast with query: {}", query);
        String prompt = promptProvider.getWeatherForecastPrompt(query);
        log.debug("Generated prompt: {}", prompt);
        String result = chatClient.prompt().user(prompt).tools(weatherPredictionTool).call().content();
        log.info("Forecast result: {}", result);
        return result;
    }

    public String performForecast(String weatherCodeQuery) {
        log.info("Performing forecast with query string: {}", weatherCodeQuery);
        String prompt = promptProvider.getWeatherForecastPrompt(weatherCodeQuery);
        log.debug("Generated prompt: {}", prompt);
        String result = chatClient.prompt().user(prompt).tools(weatherPredictionTool).call().content();
        log.info("Forecast result: {}", result);
        return result;
    }

    public String performForecastTransform(String forecastResult) {
        log.info("Transforming forecast result: {}", forecastResult);
        String prompt = promptProvider.getForecastTransformPrompt(forecastResult);
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
