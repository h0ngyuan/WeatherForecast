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

    public String getSemanticTransformPrompt(String question, String ragContext) {
        return """
            你是一个天气语义转化专家。请根据用户的问题和检索到的知识，将用户的问题转化为更精确的天气查询语句。

            用户问题: %s

            相关知识:
            %s

            转化规则:
            1. 分析用户问题中的关键词（如'晒被子'、'洗车'等），根据相关知识判断需要什么天气条件
            2. 如果用户提到具体活动，将其转化为对应的天气条件（如'晒被子' -> '晴天'）
            3. 如果用户没有明确时间，默认为'未来三天内'
            4. 如果用户没有明确地点，保持原样或使用'当前地点'
            5. 如果无法根据上述规则进行转化，请返回'未来三天内天气'
            6. 只返回转化后的问题，不要包含任何解释

            请输出转化后的问题:
            """.formatted(question, ragContext);
    }

    public String getNormalizationPrompt(String question) {
        return """
            你是一个天气语义规范化专家。请将用户的问题规范化为标准的JSON格式。

            用户问题: %s

            规范化规则:
            1. 提取问题中的时间信息（如'今天'、'明天'、'未来三天'等）
            2. 提取问题中的天气条件（如'晴天'、'雨天'、'多云'等）
            3. 提取问题中的地点信息（如'北京'、'上海'、'南通'等）
            4. 如果没有明确时间，默认为'未来三天'
            5. 如果没有明确天气条件，保持原样
            6. 如果没有明确地点，保持原样或使用'当前地点'
            7. 只返回规范化后的问题，不要包含任何解释

            请输出规范化后的问题:
            """.formatted(question);
    }
}