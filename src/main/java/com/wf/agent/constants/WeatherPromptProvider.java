package com.wf.agent.constants;

import com.wf.object.query.WeatherCodeQuery;
import org.springframework.stereotype.Component;

@Component
public class WeatherPromptProvider {

    public String getRelevanceJudgePrompt(String question) {
        return """
            请判断以下问题是否与以下领域相关：
            1. 天气相关：天气、气象、气候、温度、降水、风力、湿度等
            2. 天气预警相关：是否需要带伞、是否适合晒被子/洗车/户外活动、穿衣建议等与天气变化相关的活动决策

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

    public String getAnswerGenerationPrompt(String originalQuestion, String normalizedQuestion, String forecastResult) {
        return """
            你是一个专业的气象专家，请根据以下信息给出准确、简洁、自然的回答。

            用户原始问题: %s

            规范化问题（包含精确时间范围）: %s

            天气预测结果: %s

            回答要求:
            1. 回答要自然流畅，符合用户的原始提问方式
            2. 基于天气预测结果给出具体的建议或回答
            3. 如果用户问的是活动建议（如"能不能晒被子"），要给出明确的建议
            4. 回答要简洁明了，不要过于冗长
            5. 保持友好和专业的语气

            请给出回答:
            """.formatted(originalQuestion, normalizedQuestion, forecastResult != null ? forecastResult : "未获取到天气预测数据");
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

           
            大类	WMO代码范围	包含天气现象
            晴/无显著天气	00-03	晴、少云、云量无变化
            霾/沙尘/浮尘	04-05	霾、烟尘、沙尘悬浮
            轻雾/雾	10, 30-35	轻雾、雾、雾凇
            毛毛雨	50-59	轻/中/重毛毛雨、冻毛毛雨
            雨	60-69	轻/中/大雨、冻雨、雨夹雪
            雪	70-79	轻/中/大雪、冰丸、霰
            阵性降水	80-90	阵雨、阵雪、冰雹、雷暴伴降水
            雷暴	91-99	干雷暴、雷暴伴雨/雪/冰雹、龙卷

            转化规则:
            1. 分析用户问题中的关键词，识别用户关心的天气条件和活动类型：
               - 天气查询类：直接询问天气状况（如"今天天气怎么样"）
               - 活动决策类：询问是否适合某项活动，需要转化为天气条件查询

            2. 活动与天气条件的对应关系：
               - 带伞/雨伞 -> 是否有降雨
               - 晒被子/晾衣服 -> 是否晴天/无降雨
               - 洗车 -> 是否晴天或少云/无降雨
               - 户外活动/跑步/散步 -> 是否晴天或多云/无降雨
               - 穿衣 -> 温度范围
               - 出行/旅游 -> 整体天气状况

            3. 转化格式：
               - 天气查询类："{地点} {时间} 的天气状况"
               - 活动决策类："{地点} {时间} 是否 {天气条件}，用于{活动类型}决策"

            4. 如果用户提到模糊时间（如'明天'、'后天'、'今天'等），保持原样
            5. 如果用户没有明确地点，使用'当前地点'

            示例：
            - "明天要带伞吗" -> "当前地点 明天 是否有降雨，用于带伞决策"
            - "后天能晒被子吗" -> "当前地点 后天 是否晴天/无降雨，用于晒被子决策"
            - "我在上海明天上午可以晒被子吗？" -> "上海 明天 上午 是否晴天/无降雨，用于晒被子决策"
            - "今天天气怎么样" -> "当前地点 今天 的天气状况"
            - "后天去洗车怎么样" -> "当前地点 后天 是否晴天或少云，用于洗车决策"

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
            3. timeTool.acquireFormatHourTime(amount, unit) - 获取时间（amount为负数表示过去，正数表示未来，0表示现在）

            规范化规则:
            1. 分析用户问题类型：
               - 天气查询类：直接询问天气状况
               - 活动决策类：询问是否适合某项活动（带伞、晒被子、洗车等），需要提取活动类型

            2. 分析地点信息：
               - 如果用户问题中有明确的城市名称，使用 locationTool.hasThisCity(city) 判断该城市是否在系统中
               - 如果城市不在系统中或没有明确地点，使用 locationTool.getNearestAvailableCity() 获取最近的可预测城市

            3. 分析时间信息：
               - 调用 timeTool.acquireFormatHourTime(0, TimeUnit.DAYS) 获取当前时间
               - 如果问题中有模糊时间，计算精确时间范围：
                 * '今天' -> 当天00:00:00到23:59:59
                 * '明天' -> 下一天00:00:00到23:59:59
                 * '后天' -> 下两天00:00:00到23:59:59
                 * '明天上午' -> 下一天08:00:00到12:00:00
                 * '明天下午' -> 下一天14:00:00到18:00:00
                 * '明天晚上' -> 下一天18:00:00到22:00:00

            4. 提取活动类型和关心的天气条件：
               - 带伞 -> 关心是否有降雨
               - 晒被子/晾衣服 -> 关心是否晴天/无降雨
               - 洗车 -> 关心是否晴天或少云
               - 户外活动 -> 关心整体天气状况

            5. 输出JSON格式：
            {
              "normalizedQuestion": "规范化问题，包含时间范围、城市、天气条件",
              "requestInfo": {
                "beginTime": "开始时间，格式：yyyy-MM-dd HH:mm:ss",
                "endTime": "结束时间，格式：yyyy-MM-dd HH:mm:ss",
                "city": "可预测城市"
              },
              "activityType": "活动类型（如：带伞决策、晒被子决策等，天气查询类为null）",
              "concernCondition": "关心的天气条件（如：是否有降雨、是否晴天等）"
            }

            示例：
            用户问题："明天要带伞吗"
            调用locationTool.getNearestAvailableCity()获取：成都
            调用timeTool.acquireFormatHourTime(0, TimeUnit.DAYS)获取当前日期
            计算明天：2026-02-05 00:00:00到2026-02-05 23:59:59
            输出：
            {
              "normalizedQuestion": "2026-02-05 00:00:00到2026-02-05 23:59:59之间 成都 是否有降雨",
              "requestInfo": {
                "beginTime": "2026-02-05 00:00:00",
                "endTime": "2026-02-05 23:59:59",
                "city": "成都"
              },
              "activityType": "带伞决策",
              "concernCondition": "是否有降雨"
            }

            用户问题："后天能晒被子吗"
            输出：
            {
              "normalizedQuestion": "2026-02-06 00:00:00到2026-02-06 23:59:59之间 成都 是否晴天/无降雨",
              "requestInfo": {
                "beginTime": "2026-02-06 00:00:00",
                "endTime": "2026-02-06 23:59:59",
                "city": "成都"
              },
              "activityType": "晒被子决策",
              "concernCondition": "是否晴天/无降雨"
            }

            用户问题："今天天气怎么样"
            输出：
            {
              "normalizedQuestion": "2026-02-04 00:00:00到2026-02-04 23:59:59之间 成都 的天气状况",
              "requestInfo": {
                "beginTime": "2026-02-04 00:00:00",
                "endTime": "2026-02-04 23:59:59",
                "city": "成都"
              },
              "activityType": null,
              "concernCondition": null
            }

            请输出规范化后的JSON:
            """.formatted(question);
    }

    public String getWeatherForecastPrompt(WeatherCodeQuery query) {
        return """
            你是一个天气分析专家。请根据以下查询信息，调用天气预测工具获取数据。
            
            查询信息:
            - 城市: %s
            - 开始时间: %s
            - 结束时间: %s
            
            请按照以下步骤操作：
            1. 调用 acquireWeatherCodeValueByRangeTime 工具获取天气数据
            2. 直接返回工具返回的原始数据，不要进行任何转化或分析
            
            返回格式：
            - 直接返回工具获取到的天气码列表，按逗号分隔
            - 不要添加任何描述或解释
            
            示例：
            - 工具返回 [100, 100, 100] -> "100,100,100"
            - 工具返回 [100, 101, 100] -> "100,101,100"
            
            请直接返回天气码列表，不要包含其他解释。
            """.formatted(query.getLocation(), query.getBeginTime(), query.getEndTime());
    }

    public String getWeatherForecastPrompt(String weatherCodeQuery) {
        return """
            你是一个天气分析专家。请根据以下查询信息，调用天气预测工具获取数据。
            
            查询信息: %s
            
            请按照以下步骤操作：
            1. 调用 acquireWeatherCodeValueByRangeTime 工具获取天气数据
            2. 直接返回工具返回的原始数据，不要进行任何转化或分析
            
            返回格式：
            - 直接返回工具获取到的天气码列表，按逗号分隔
            - 不要添加任何描述或解释
            
            示例：
            - 工具返回 [100, 100, 100] -> "100,100,100"
            - 工具返回 [100, 101, 100] -> "100,101,100"
            
            请直接返回天气码列表，不要包含其他解释。
            """.formatted(weatherCodeQuery);
    }

    public String getForecastTransformPrompt(String forecastResult) {
        return """
            你是一个天气语义转化专家。请将以下天气预测数据转化为更自然、更易理解的描述。

            原始预测数据: %s
            
            请按照以下步骤操作：
            1. 分析原始数据中的天气码序列
            2. 调用 getWeatherCodes 工具将天气码转换为可读的天气描述
            3. 根据转换后的天气描述和时间序列，生成自然的语言表达
            
            转化规则：
            - 将相同天气状况的时间段合并描述
            - 使用自然的时间表达方式（如"9点到10点"、"10点到14点"等）
            - 描述要简洁流畅，符合用户阅读习惯
            
            示例：
            - "100,100,100" -> 调用工具获得["晴天","晴天","晴天"] -> "全天天气晴朗"
            - "100,100,101,100,100,100" -> 调用工具获得["晴天","晴天","多云","晴天","晴天","晴天"] -> "上午到下午天气晴朗，傍晚转多云"
            - "9点到10点是阴天，10点到14点是晴天，14点到15点是晴天" -> "9点到10点是阴天，10点到15点是晴天"
            
            请直接返回转化后的描述，不要包含其他解释。
            """.formatted(forecastResult);
    }

    public String getAlertCheckPrompt(String originalQuestion, String forecastResult, String activityType, String concernCondition) {
        return """
            你是一个天气预警分析专家。请根据用户的活动类型和关心的天气条件，分析是否需要创建天气提醒任务。

            用户原始问题: %s

            天气预测结果: %s

            活动类型: %s

            关心的天气条件: %s

            核心逻辑说明：
            天气预报具有不确定性，当前预测可能与实际情况不符。当用户询问特定天气相关问题时，
            意味着用户关心该天气情况，需要系统持续监控天气变化，并在天气条件改变时通知用户。

            分析规则：
            1. 如果活动类型不为空，说明用户需要针对该活动进行天气决策，必须创建提醒任务（hasAlert = true）
            2. 如果活动类型为空，说明是普通天气查询，不需要创建提醒任务（hasAlert = false）

            提醒任务信息：
            - taskType: 直接使用传入的活动类型
            - concernCondition: 直接使用传入的关心条件
            - currentPrediction: 根据天气预测结果填写
            - monitoringPeriod: 根据用户问题中的时间推断
            - notifyCondition: 根据关心的条件设置触发通知的条件

            输出格式（JSON）:
            {
              "hasAlert": true/false,
              "alertLevel": "无/低/中/高/极高",
              "alertType": ["降雨", "高温", "低温", "大风", "暴雪", "雷电"],
              "alertMessage": "预警或提醒描述",
              "suggestion": "给用户的建议",
              "reminderTask": {
                "taskType": "任务类型",
                "concernCondition": "用户关心的天气条件",
                "currentPrediction": "当前预测结果",
                "monitoringPeriod": "监控时间范围",
                "notifyCondition": "触发通知的条件"
              }
            }

            示例：
            - 活动类型="带伞决策"，关心条件="是否有降雨"，预测晴天：
            {
              "hasAlert": true,
              "alertLevel": "低",
              "alertType": ["降雨"],
              "alertMessage": "当前预测无降雨，但天气可能变化",
              "suggestion": "根据当前预测不需要带伞，但建议出门前再次确认天气",
              "reminderTask": {
                "taskType": "带伞决策",
                "concernCondition": "是否有降雨",
                "currentPrediction": "晴天，无降雨",
                "monitoringPeriod": "用户指定时间之前",
                "notifyCondition": "预测变为有降雨"
              }
            }

            - 活动类型="晒被子决策"，关心条件="是否晴天/无降雨"，预测有雨：
            {
              "hasAlert": true,
              "alertLevel": "中",
              "alertType": ["降雨"],
              "alertMessage": "预测有降雨天气，不适合晒被子",
              "suggestion": "建议选择其他时间晒被子",
              "reminderTask": {
                "taskType": "晒被子决策",
                "concernCondition": "是否晴天/无降雨",
                "currentPrediction": "有降雨",
                "monitoringPeriod": "用户指定时间之前",
                "notifyCondition": "预测变为晴天"
              }
            }

            - 活动类型=null（普通天气查询）：
            {
              "hasAlert": false,
              "alertLevel": "无",
              "alertType": [],
              "alertMessage": "无特殊预警",
              "suggestion": "",
              "reminderTask": null
            }

            请直接返回JSON格式的结果，不要包含其他解释。
            """.formatted(originalQuestion, forecastResult != null ? forecastResult : "未获取到天气预测数据",
                         activityType != null ? activityType : "无",
                         concernCondition != null ? concernCondition : "无");
    }

    public String getFinalGeneratePrompt(String originalQuestion, String normalizedQuestion, String forecastResult, String generateResult, String alertCheckResult) {
        return """
            你是一个专业的气象专家，请根据以下信息给出准确、简洁、自然的最终回答。

            用户原始问题: %s

            规范化问题（包含精确时间范围）: %s

            天气预测结果: %s

            初步生成答案: %s

            预警检查结果: %s

            回答要求:
            1. 基于初步生成答案，结合预警检查结果给出最终回答
            2. 如果预警检查结果中有预警信息（hasAlert为true），必须在回答中包含预警内容和建议
            3. 如果预警检查结果中无预警信息，直接使用初步生成答案
            4. 回答要自然流畅，符合用户的原始提问方式
            5. 保持友好和专业的语气

            请给出最终回答:
            """.formatted(originalQuestion, normalizedQuestion, forecastResult != null ? forecastResult : "未获取到天气预测数据", generateResult != null ? generateResult : "未生成初步答案", alertCheckResult != null ? alertCheckResult : "未进行预警检查");
    }
}