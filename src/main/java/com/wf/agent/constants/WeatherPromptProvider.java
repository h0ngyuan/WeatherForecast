package com.wf.agent.constants;

import org.springframework.stereotype.Component;

@Component
public class WeatherPromptProvider {

    public String getRelevanceJudgePrompt(String question) {
        return """
            请判断以下问题是否与天气、气象、气候、温度、降水、风力等相关。
            仅返回一个0到1之间的浮点数，表示相关性评分，不要包含任何其他文字。
            问题：%s
            """.formatted(question);
    }

    public String getAnswerGenerationPrompt(String question) {
        return """
            你是一个专业的气象专家，请根据以下问题给出准确、简洁的回答：
            %s
            """.formatted(question);
    }

    public String getAnswerQualityScorePrompt(String question, String answer) {
        return """
            请对你刚才生成的回答质量进行评分（0~1之间，1为完美）。只返回一个浮点数，不要任何解释。
            问题：%s
            回答：%s
            """.formatted(question, answer);
    }
}