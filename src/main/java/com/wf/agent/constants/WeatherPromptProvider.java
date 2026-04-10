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

    public String getAnswerGenerationPrompt(String originalQuestion, String normalizedQuestion, String forecastResult, String activityType, String concernCondition) {
        return """
            你是一个专业的气象专家，请综合分析以下所有信息，给出准确、简洁、自然的回答。

            【用户原始问题】
            %s

            【语义转化后的规范化问题】
            %s

            【天气预测数据】
            %s

            【活动类型】
            %s

            【用户关心的天气条件】
            %s

            【综合分析要求】
            1. 结合用户原始问题的意图和语义转化后的精确时间范围
            2. 基于天气预测数据，针对用户关心的天气条件给出分析
            3. 如果涉及活动决策（如带伞、晒被子等），给出明确的是/否建议及理由
            4. 考虑天气预报的不确定性，给出合理的提醒（如"建议随时关注天气变化"）
            5. 回答要自然流畅，符合用户的提问方式，不要过于技术化
            6. 保持友好、专业、简洁的语气

            请给出综合分析和建议:
            """.formatted(
                originalQuestion, 
                normalizedQuestion != null ? normalizedQuestion : "未提供", 
                forecastResult != null ? forecastResult : "未获取到天气预测数据",
                activityType != null && !activityType.isEmpty() ? activityType : "一般天气查询",
                concernCondition != null && !concernCondition.isEmpty() ? concernCondition : "整体天气状况"
            );
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

            【最美天气自定义代码（WID）映射表 - MCP服务返回的数据使用此编码】
            WID	天气现象	降水概率参考	说明
            1	晴	0~10%%	无降水，适合户外活动
            7	多云	0~20%%	云量较多但无降水
            8	阴	10~30%%	云层厚但无降水
            15	雷阵雨	60~90%%	强对流天气，有雷电
            33	雾/轻雾	-	能见度<10km
            46	小雨	70~90%%	日常降雨
            47	中雨	80~95%%	持续降水
            48	大雨	90~100%%	强降水预警
            49	暴雨	≥90%%	极端天气，≥50mm
            75	霾/沙尘	-	空气质量差

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
               - 如果用户问题中有明确的城市名称，调用 locationTool.getCityLocation(city) 获取城市经纬度
               - 如果返回结果中包含 "needAiHelp": true，说明API无法获取该城市经纬度：
                 * 你需要根据你的知识直接提供该城市的经纬度
                 * 然后调用 locationTool.saveCityToDatabase(city, latitude, longitude) 将城市信息保存到数据库
               - 如果没有明确地点，调用 locationTool.getNearestAvailableCity() 获取IP定位的城市和经纬度（该方法也会自动将城市添加到数据库）

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
                "city": "可预测城市",
                "latitude": 纬度（数字）,
                "longitude": 经度（数字）
              },
              "activityType": "活动类型（如：带伞决策、晒被子决策等，天气查询类为null）",
              "concernCondition": "关心的天气条件（如：是否有降雨、是否晴天等）"
            }

            示例：
            用户问题："明天要带伞吗"
            没有明确地点，调用locationTool.getNearestAvailableCity()获取：{"city":"成都","latitude":30.5728,"longitude":104.0668}
            调用timeTool.acquireFormatHourTime(0, TimeUnit.DAYS)获取当前日期
            计算明天：2026-02-05 00:00:00到2026-02-05 23:59:59
            输出：
            {
              "normalizedQuestion": "2026-02-05 00:00:00到2026-02-05 23:59:59之间 成都 是否有降雨",
              "requestInfo": {
                "beginTime": "2026-02-05 00:00:00",
                "endTime": "2026-02-05 23:59:59",
                "city": "成都",
                "latitude": 30.5728,
                "longitude": 104.0668
              },
              "activityType": "带伞决策",
              "concernCondition": "是否有降雨"
            }

            用户问题："北京明天能晒被子吗"
            明确地点"北京"，调用locationTool.getCityLocation("北京")获取：{"city":"北京","latitude":39.9042,"longitude":116.4074}
            调用timeTool.acquireFormatHourTime(0, TimeUnit.DAYS)获取当前日期
            计算明天：2026-02-05 00:00:00到2026-02-05 23:59:59
            输出：
            {
              "normalizedQuestion": "2026-02-05 00:00:00到2026-02-05 23:59:59之间 北京 是否晴天/无降雨",
              "requestInfo": {
                "beginTime": "2026-02-05 00:00:00",
                "endTime": "2026-02-05 23:59:59",
                "city": "北京",
                "latitude": 39.9042,
                "longitude": 116.4074
              },
              "activityType": "晒被子决策",
              "concernCondition": "是否晴天/无降雨"
            }

            用户问题："某小众城市明天天气怎么样"
            明确地点"某小众城市"，调用locationTool.getCityLocation("某小众城市")返回：{"city":"某小众城市","needAiHelp":true,"message":"无法通过API获取某小众城市的经纬度..."}
            由于needAiHelp为true：
              - 你根据知识提供该城市经纬度：latitude=25.1234, longitude=118.5678
              - 调用locationTool.saveCityToDatabase("某小众城市", 25.1234, 118.5678)保存到数据库
            调用timeTool.acquireFormatHourTime(0, TimeUnit.DAYS)获取当前日期
            计算明天：2026-02-05 00:00:00到2026-02-05 23:59:59
            输出：
            {
              "normalizedQuestion": "2026-02-05 00:00:00到2026-02-05 23:59:59之间 某小众城市 的天气状况",
              "requestInfo": {
                "beginTime": "2026-02-05 00:00:00",
                "endTime": "2026-02-05 23:59:59",
                "city": "某小众城市",
                "latitude": 25.1234,
                "longitude": 118.5678
              },
              "activityType": null,
              "concernCondition": null
            }

            用户问题："今天天气怎么样"
            没有明确地点，调用locationTool.getNearestAvailableCity()获取：{"city":"成都","latitude":30.5728,"longitude":104.0668}
            输出：
            {
              "normalizedQuestion": "2026-02-04 00:00:00到2026-02-04 23:59:59之间 成都 的天气状况",
              "requestInfo": {
                "beginTime": "2026-02-04 00:00:00",
                "endTime": "2026-02-04 23:59:59",
                "city": "成都",
                "latitude": 30.5728,
                "longitude": 104.0668
              },
              "activityType": null,
              "concernCondition": null
            }

            请输出规范化后的JSON:
            """.formatted(question);
    }

    public String getWeatherForecastPrompt(WeatherCodeQuery query, String weatherCodes) {
        return """
            你是一个天气分析专家。请根据以下查询信息和天气数据进行分析。

            查询信息:
            - 城市: %s
            - 开始时间: %s
            - 结束时间: %s
            
            天气码数据: %s
            
            请直接返回天气码数据，不要添加任何描述或解释。
            """.formatted(query.getLocation(), query.getBeginTime(), query.getEndTime(), 
                    weatherCodes != null ? weatherCodes : "无数据");
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
            
            【天气码解释规则 - 最美天气WID】
            
            数据来自MCP服务，使用的是"最美天气自定义代码（wid）"，映射如下：
            wid  天气现象        降水概率    说明
            1    晴              0~10%%      无降水，适合户外活动
            7    多云            0~20%%      云量较多但无降水
            8    阴              10~30%%     云层厚但无降水
            15   雷阵雨          60~90%%     强对流，有雷电
            33   雾/轻雾         -           能见度<10km
            46   小雨            70~90%%     日常降雨
            47   中雨            80~95%%     持续降水
            48   大雨            90~100%%    强降水
            49   暴雨            ≥90%%       极端天气，≥50mm
            75   霾/沙尘         -           空气质量差
            请按照以下步骤操作：
            1. 分析原始数据中的天气码序列（最美天气WID格式）
            2. 根据上述WID映射表，将每个天气码转换为对应的中文天气描述
            3. 根据转换后的天气描述和时间序列，生成自然的语言表达
            
            转化规则：
            - 将相同天气状况的时间段合并描述
            - 使用自然的时间表达方式（如"9点到10点"、"10点到14点"等）
            - 描述要简洁流畅，符合用户阅读习惯
            - 只需如实翻译天气码对应的天气现象，不需要添加预警或建议
            
            示例：
            - "1,1,1" -> 全天晴
            - "33,33,1,1,46,46" -> 清晨至上午有雾，中午晴，下午小雨
            - "1,7,7,8,46,46" -> 上午晴转多云，中午阴，下午小雨
            - "46,47,48,48,49,49" -> 上午小雨转中雨，中午大雨，下午暴雨
            
            请直接返回转化后的描述，不要包含其他解释。
            """.formatted(forecastResult);
    }

    public String getAlertCheckPrompt(String originalQuestion, String forecastResult, String activityType, String concernCondition) {
        return """
            你是一个天气提醒任务分析专家。请根据以下通用规则判断是否需要创建提醒任务。

            【输入信息】
            用户原始问题: %s
            天气预测结果: %s
            活动类型: %s
            关心的天气条件: %s

            【核心判断逻辑】

            判断用户意图类型：

            1. 活动决策型（必须创建任务）
               特征：用户需要根据天气做某个具体决定
               判断条件：活动类型不为空
               处理方式：hasAlert=true, taskType=活动类型

            2. 时间关注型（必须创建任务）
               特征：用户关心某种天气现象何时发生（而非当前是否发生）
               判断条件：问题中包含时间询问词 + 天气现象词
               时间询问词：什么时候、何时、啥时候、多久
               天气现象词：雨、雪、风、降温、升温、雾、霾等
               处理方式：hasAlert=true, taskType="天气关注"

            3. 状态查询型（不创建任务）
               特征：用户只想了解当前或未来某时的天气状况
               判断条件：问题只询问天气状态，无时间关注或活动决策
               关键词：天气怎么样、天气如何、气温多少、会下雨吗（仅问当前）
               处理方式：hasAlert=false

            【判断公式】
            hasAlert = (活动类型 != null) OR (包含时间询问词 AND 包含天气现象词)

            【输出格式】
            {
              "hasAlert": true/false,
              "alertLevel": "无/低/中/高/极高",
              "alertType": ["降雨", "高温", "低温", "大风", "暴雪", "雷电"],
              "alertMessage": "当前天气状况说明",
              "suggestion": "给用户的建议",
              "reminderTask": {
                "taskType": "任务类型",
                "concernCondition": "关心的天气条件",
                "currentPrediction": "当前预测",
                "monitoringPeriod": "监控时间",
                "notifyCondition": "通知触发条件",
                "disasterLevel": 1/2/3
              }
            }

            【disasterLevel评定】
            - 1级：暴雨、暴雪、台风、极端温度
            - 2级：大雨、大风、雷电、大雾
            - 3级：小雨、多云、一般天气

            【通用处理原则】
            - 用户需要"决策"或"关注时间" → 创建任务
            - 用户只是"查询状态" → 不创建任务
            - 不确定时，优先不创建任务

            请直接返回JSON，不要其他解释。
            """.formatted(originalQuestion, forecastResult != null ? forecastResult : "未获取到天气预测数据",
                         activityType != null ? activityType : "无",
                         concernCondition != null ? concernCondition : "无");
    }

    public String getFinalGeneratePrompt(String originalQuestion, String normalizedQuestion, String forecastResult, String generateResult, String alertCheckResult) {
        String basePrompt = """
            你是一个专业的气象专家，请根据以下信息给出准确、简洁、自然的最终回答。

            用户原始问题: %s

            规范化问题（包含精确时间范围）: %s

            天气预测结果: %s

            预警检查结果: %s

            回答要求:
            1. 基于天气预测结果，结合预警检查结果给出最终回答
            2. 如果预警检查结果中有预警信息（hasAlert为true）：
               - 必须在回答中包含预警内容和建议
               - 如果reminderTask不为null，必须明确告知用户"已为您记录天气关注任务，当天气条件变化时会及时通知您"
            3. 如果预警检查结果中无预警信息，直接基于天气预测结果回答
            4. 回答要自然流畅，符合用户的原始提问方式
            5. 保持友好和专业的语气

            请给出最终回答:
            """;
        
        if (generateResult != null && !generateResult.isEmpty() && !generateResult.equals("未生成初步答案")) {
            basePrompt = """
                你是一个专业的气象专家，请根据以下信息给出准确、简洁、自然的最终回答。

                用户原始问题: %s

                规范化问题（包含精确时间范围）: %s

                天气预测结果: %s

                初步生成答案: %s

                预警检查结果: %s

                回答要求:
                1. 基于初步生成答案，结合预警检查结果给出最终回答
                2. 如果预警检查结果中有预警信息（hasAlert为true）：
                   - 必须在回答中包含预警内容和建议
                   - 如果reminderTask不为null，必须明确告知用户"已为您记录天气关注任务，当天气条件变化时会及时通知您"
                3. 如果预警检查结果中无预警信息，直接使用初步生成答案
                4. 回答要自然流畅，符合用户的原始提问方式
                5. 保持友好和专业的语气

                请给出最终回答:
                """;
            return basePrompt.formatted(originalQuestion, normalizedQuestion, 
                forecastResult != null ? forecastResult : "未获取到天气预测数据", 
                generateResult, 
                alertCheckResult != null ? alertCheckResult : "未进行预警检查");
        }
        
        return basePrompt.formatted(originalQuestion, normalizedQuestion, 
            forecastResult != null ? forecastResult : "未获取到天气预测数据", 
            alertCheckResult != null ? alertCheckResult : "未进行预警检查");
    }
}