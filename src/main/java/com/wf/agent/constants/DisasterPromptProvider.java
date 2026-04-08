package com.wf.agent.constants;

import com.wf.agent.entity.DisasterInfo;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 灾害评估Prompt提供者
 *
 * 职责：
 * 统一管理灾害评估相关的所有Prompt
 *
 * @author author
 * @since 1.0.0
 */
@Component
public class DisasterPromptProvider {

    // 最美天气自定义代码（wid）映射表 - MCP服务使用
    private static final Map<Integer, String> WID_CODE_MAP = new HashMap<>();

    static {
        WID_CODE_MAP.put(1, "晴 - 无降水，适合户外活动");
        WID_CODE_MAP.put(7, "多云 - 云量较多但无降水");
        WID_CODE_MAP.put(8, "阴 - 云层厚但无降水");
        WID_CODE_MAP.put(15, "雷阵雨 - 强对流天气，有雷电");
        WID_CODE_MAP.put(33, "雾/轻雾 - 能见度<10km");
        WID_CODE_MAP.put(46, "小雨 - 日常降雨，0.1~10mm");
        WID_CODE_MAP.put(47, "中雨 - 持续降水，10~25mm");
        WID_CODE_MAP.put(48, "大雨 - 强降水，25~50mm");
        WID_CODE_MAP.put(49, "暴雨 - 极端天气，≥50mm");
        WID_CODE_MAP.put(75, "霾/沙尘 - 空气质量差");
    }

    /**
     * 获取灾害分析Prompt（分析Agent）
     */
    public String getDisasterAnalysisPrompt(String location, List<Integer> weatherCodes) {
        String codeTable = buildWeatherCodeTable(weatherCodes);

        return """
            你是一位气象灾害分析专家。请分析以下24小时天气数据，识别可能的灾害。

            【分析任务】
            地区：%s
            24小时天气码值：%s

            【天气码值对照表 - 最美天气自定义代码（wid）】
            %s

            【灾害识别规则】
            - 1(晴天): 无影响
            - 7(多云): 无影响
            - 8(阴天): 无影响
            - 46(小雨): 轻微影响，一般不构成灾害
            - 47(中雨): 持续降水，可能影响户外活动
            - 48(大雨): 强降水预警，构成灾害
            - 49(暴雨): 极端天气，严重灾害
            - 15(雷阵雨): 强对流天气，有雷电风险
            - 33(雾/轻雾): 能见度低，影响交通
            - 75(霾/沙尘): 空气质量差，影响健康

            【输出要求】
            请返回JSON格式：
            {
              "disasters": [
                {
                  "type": "灾害类型（必填，不能为空）",
                  "weatherCode": 天气码值,
                  "startHour": 开始小时(0-23),
                  "endHour": 结束小时(0-23),
                  "description": "灾害描述和依据"
                }
              ]
            }
            
            【重要】type字段必须从以下选择（根据weatherCode对应）：
            - weatherCode=15 -> type="雷阵雨"
            - weatherCode=33 -> type="雾/轻雾"
            - weatherCode=46 -> type="小雨"
            - weatherCode=47 -> type="中雨"
            - weatherCode=48 -> type="大雨"
            - weatherCode=49 -> type="暴雨"
            - weatherCode=75 -> type="霾/沙尘"
            
            无灾害则返回 {"disasters": []}
            """.formatted(location, weatherCodes, codeTable);
    }

    /**
     * 获取灾害评审Prompt（评审Agent）
     */
    public String getDisasterReviewPrompt(String location, List<Integer> weatherCodes, DisasterInfo disaster) {
        return """
            你是一位气象灾害评审专家。请对另一位专家的分析结论进行独立评审。

            【原始数据】
            地区：%s
            24小时天气码值：%s

            【分析专家的结论】
            灾害类型：%s
            涉及天气码值：%d
            时间段：第%d小时 至 第%d小时
            分析依据：%s

            【评审任务】
            1. 验证该灾害是否真实存在（天气数据是否支持）
            2. 如存在，评定级别：1=严重（需全员通知），2=中等，3=轻微

            【分级参考】
            - 1级（严重）：台风、特大暴雨、暴雪、冰雹、极端大风
            - 2级（中等）：大雨、大风、雷电、大雾
            - 3级（轻微）：小雨、多云、阴天

            【输出要求】
            返回JSON格式：
            {
              "valid": true/false,
              "level": 1/2/3,
              "reason": "评审意见"
            }
            """.formatted(
                location, weatherCodes,
                disaster.getType(), disaster.getWeatherCode(),
                disaster.getStartHour(), disaster.getEndHour(),
                disaster.getDescription()
            );
    }

    /**
     * 构建天气码值对照表（只包含实际出现的码值）
     */
    private String buildWeatherCodeTable(List<Integer> weatherCodes) {
        return weatherCodes.stream()
                .distinct()
                .map(code -> {
                    String desc = WID_CODE_MAP.get(code);
                    return desc != null ? code + "=" + desc : code + "=未知";
                })
                .collect(Collectors.joining("，\n"));
    }
}
