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

    public String getAnswerGenerationPrompt(String originalQuestion, String normalizedQuestion, String locationInfo) {
        return """
            你是一个专业的气象专家，请根据以下信息给出准确、简洁、自然的回答。

            用户原始问题: %s

            规范化问题（包含精确时间范围）: %s

            位置信息: %s

            回答要求:
            1. 回答要自然流畅，符合用户的原始提问方式
            2. 使用规范化问题中的精确时间信息来查询天气数据
            3. 根据天气数据给出具体的建议或回答
            4. 如果用户问的是活动建议（如"能不能晒被子"），要给出明确的建议
            5. 回答要简洁明了，不要过于冗长
            6. 保持友好和专业的语气

            请给出回答:
            """.formatted(originalQuestion, normalizedQuestion, locationInfo != null ? locationInfo : "未指定");
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
            你是一个天气语义转化专家。请根据用户的问题和检索到的知识，将用户的问题转化为天气条件查询。

            用户问题: %s

            相关知识:
            %s

            转化规则:
            1. 分析用户问题中的关键词（如'晒被子'、'洗车'、'跑步'等），根据相关知识判断需要什么天气条件
            2. 将活动转化为对应的天气条件，格式为："{地点} {时间} 是 {天气条件} 吗？"
            3. 常见活动与天气条件的对应关系：
               - 晒被子 -> 晴天
               - 洗车 -> 晴天或少云
               - 跑步 -> 晴天或多云
               - 雨天出门 -> 雨天
               - 滑雪 -> 下雪
               - 看彩虹 -> 雨后
            4. 如果用户提到模糊时间（如'明天'、'后天'、'今天'等），保持原样，不要转化为具体时间
            5. 如果用户没有明确时间，保持原样
            6. 如果用户没有明确地点，保持原样或使用'当前地点'
            7. 如果无法根据上述规则进行转化，请返回原问题
            8. 只返回转化后的问题，不要包含任何解释

            示例：
            - "我在上海明天上午可以晒被子吗？" -> "上海 明天 上午 是 晴天 吗？"
            - "后天去洗车怎么样" -> "后天 是 晴天或少云 吗？"
            - "今天晚上能不能跑步" -> "今天 晚上 是 晴天或多云 吗？"

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
            """;
    }

    public String getCompleteNormalizationPrompt(String question) {
        return """
            你是一个天气语义规范化专家。请将用户的问题规范化为标准的JSON格式。

            用户问题: %s

            可用工具:
            1. locationTool.getNearestAvailableCity() - 获取离当前位置最近的可预测城市
            2. locationTool.hasThisCity(city) - 判断系统中是否有指定城市的数据（返回 true/false）
            3. timeTool.getCurrentTime() - 获取当前时间（返回格式：{"currentTime":"2026-02-04 14:30:00","beginTime":"2026-02-04 14:00:00","endTime":"2026-02-07 14:00:00"}）

            规范化规则:
            1. 分析用户问题中的地点信息：
               - 如果用户问题中有明确的城市名称（如"上海"、"北京"等），使用 locationTool.hasThisCity(city) 判断该城市是否在系统中
               - 如果 locationTool.hasThisCity(city) 返回 true，使用该城市
               - 如果 locationTool.hasThisCity(city) 返回 false，使用 locationTool.getNearestAvailableCity() 获取最近的可预测城市
               - 如果用户问题中没有明确地点，使用 locationTool.getNearestAvailableCity() 获取最近的可预测城市
            2. 调用 timeTool.getCurrentTime() 获取当前时间信息
            3. 分析用户问题中的时间信息：
               - 如果问题中没有明确时间，使用beginTime作为开始时间，endTime作为结束时间
               - 如果问题中有模糊时间（如'今天'、'明天'、'后天'），需要根据currentTime计算精确时间范围：
                 * '今天' -> 从currentTime的当天00:00:00到23:59:59
                 * '明天' -> 从currentTime的下一天00:00:00到23:59:59
                 * '后天' -> 从currentTime的下两天00:00:00到23:59:59
                 * '明天上午' -> 从currentTime的下一天08:00:00到12:00:00
                 * '明天中午' -> 从currentTime的下一天11:00:00到14:00:00
                 * '明天下午' -> 从currentTime的下一天14:00:00到18:00:00
                 * '明天晚上' -> 从currentTime的下一天18:00:00到22:00:00
                 * '今天上午' -> 从currentTime的当天08:00:00到12:00:00
                 * '今天下午' -> 从currentTime的当天14:00:00到18:00:00
                 * '今天晚上' -> 从currentTime的当天18:00:00到22:00:00
               - 如果问题中有具体时间（如'2026-02-05 14:00'），直接使用
            4. 提取问题中的天气条件（如'晴天'、'雨天'、'多云'、'少云'等）
            5. 将规范化后的内容组织成以下JSON格式：
            {
              "normalizedQuestion": "使用精确时间范围的规范化问题，格式：{开始时间}到{结束时间}之间 {城市} 的天气状况是否为 {天气条件}",
              "requestInfo": {
                "beginTime": "精确的开始时间，格式：yyyy-MM-dd HH:mm:ss",
                "endTime": "精确的结束时间，格式：yyyy-MM-dd HH:mm:ss",
                "city": "必须是系统中存在的可预测城市"
              }
            }
            6. 只返回JSON格式的结果，不要包含任何解释

            示例：
            用户问题："上海 明天 上午 是 晴天 吗？"
            调用locationTool.hasThisCity("上海")判断：false（系统中没有上海）
            调用locationTool.getNearestAvailableCity()获取：成都
            调用timeTool.getCurrentTime()获取：{"currentTime":"2026-02-04 14:30:00","beginTime":"2026-02-04 14:00:00","endTime":"2026-02-07 14:00:00"}
            计算明天上午：2026-02-05 08:00:00到2026-02-05 12:00:00
            使用城市：成都（因为上海不在系统中）
            提取天气条件：晴
            输出：
            {
              "normalizedQuestion": "2026-02-05 08:00:00到2026-02-05 12:00:00之间 成都 的天气状况是否为 晴",
              "requestInfo": {
                "beginTime": "2026-02-05 08:00:00",
                "endTime": "2026-02-05 12:00:00",
                "city": "成都"
              }
            }

            请输出规范化后的JSON:
            """.formatted(question);
    }
}