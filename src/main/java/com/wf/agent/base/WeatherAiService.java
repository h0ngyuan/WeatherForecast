package com.wf.agent.base;

import com.wf.agent.constants.WeatherPromptProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class WeatherAiService {

    private final ChatClient chatClient;
    private final WeatherPromptProvider promptProvider;

    public WeatherAiService(ChatClient.Builder chatClientBuilder, WeatherPromptProvider promptProvider) {
        this.chatClient = chatClientBuilder.build();
        this.promptProvider = promptProvider;
    }

    public double judgeRelevance(String question) {
        String prompt = promptProvider.getRelevanceJudgePrompt(question);
        return parseScore(chatClient.prompt().user(prompt).call().content());
    }

    public String generateAnswer(String question) {
        String prompt = promptProvider.getAnswerGenerationPrompt(question);
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

    private double parseScore(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
