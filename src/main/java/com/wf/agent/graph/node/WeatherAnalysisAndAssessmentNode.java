package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.base.AIClient;
import com.wf.agent.entity.DisasterInfo;
import com.wf.agent.skill.SkillRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 天气分析与灾害等级评估 Node（合并版）
 *
 * 职责：
 * 1. 分析24小时天气码，按时间段分组
 * 2. 总结当天天气（可能多个时段不同天气）
 * 3. 根据天气类型读取对应Skill
 * 4. AI使用Skill规则判定灾害等级
 *
 * 输入 State:
 *   - location: String (地区名称)
 *   - weatherCodes: List<Integer> (24小时天气码)
 *
 * 输出 State:
 *   - confirmedDisasters: List<DisasterInfo> (带等级的灾害列表)
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
public class WeatherAnalysisAndAssessmentNode implements NodeAction {

    private final AIClient aiClient;
    private final SkillRegistry skillRegistry;

    public WeatherAnalysisAndAssessmentNode(AIClient aiClient, SkillRegistry skillRegistry) {
        this.aiClient = aiClient;
        this.skillRegistry = skillRegistry;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String location = state.value("location", "unknown");
        @SuppressWarnings("unchecked")
        List<Integer> weatherCodes = state.value("weatherCodes", List.of());

        log.info("[WeatherAnalysisAndAssessmentNode] 分析 {} 的24小时天气", location);

        // 获取可用Skill列表（供AI选择）
        String skillList = skillRegistry.getSkillListForModel();

        // 构建分析Prompt
        String prompt = buildAnalysisPrompt(location, weatherCodes, skillList);

        // 调用AI分析
        String response = aiClient.chatClient()
                .prompt()
                .user(prompt)
                .call()
                .content();

        // 解析结果
        List<DisasterInfo> disasters = parseResponse(response);

        log.info("[WeatherAnalysisAndAssessmentNode] 识别到 {} 个灾害", disasters.size());
        for (DisasterInfo d : disasters) {
            log.info("  - {}: {}级 ({})", d.getType(), d.getLevel(), d.getDescription());
        }

        return Map.of("confirmedDisasters", disasters);
    }

    /**
     * 构建分析Prompt
     */
    private String buildAnalysisPrompt(String location, List<Integer> weatherCodes, String skillList) {
        StringBuilder codesStr = new StringBuilder();
        for (int i = 0; i < weatherCodes.size(); i++) {
            codesStr.append(String.format("  第%02d小时: %d\n", i, weatherCodes.get(i)));
        }

        return """
            你是一位气象灾害分析专家。请分析以下24小时天气数据，识别灾害并判定等级。

            【分析任务】
            地区：%s

            【24小时天气码序列】
            %s

            【天气码说明（最美天气自定义代码 - wid）】
            - 1: 晴（无降水）
            - 7: 多云
            - 8: 阴
            - 15: 雷阵雨（强对流，有雷电）
            - 33: 雾/轻雾（能见度<10km）
            - 46: 小雨（0.1~10mm）
            - 47: 中雨（10~25mm）
            - 48: 大雨（25~50mm）
            - 49: 暴雨（≥50mm，极端天气）
            - 75: 霾/沙尘（空气质量差）

            【分析步骤】
            1. 将24小时按连续相同天气码分组（如：0-6点晴，6-12点小雨）
            2. 总结当天天气情况（可能多个时段不同天气）
            3. 对每个有影响的天气时段：
               - 根据天气类型从下方Skill列表中选择对应Skill
               - 读取Skill规则（模拟调用 read_skill(skill_name)）
               - 使用Skill规则判定灾害等级
            4. 如果一天内有多个天气（如早上晴晚上暴雨），分别判定后取最高等级

            【可用Skill列表】
            %s

            【输出要求】
            返回JSON格式：
            {
              "disasters": [
                {
                  "type": "灾害类型（如：暴雨、雷阵雨、雾等）",
                  "weatherCode": 天气码值,
                  "startHour": 开始小时(0-23),
                  "endHour": 结束小时(0-23),
                  "level": 1/2/3 (1=严重, 2=中等, 3=轻微),
                  "description": "判定依据，引用Skill规则说明"
                }
              ],
              "weatherSummary": "当天天气总结，如：白天晴朗，夜间有大雨"
            }

            【重要】
            - 无灾害时返回 {"disasters": [], "weatherSummary": "..."}
            - level必须根据Skill规则判定，不能随意指定
            - description必须说明使用了哪个Skill的哪条规则
            """.formatted(location, codesStr.toString(), skillList);
    }

    /**
     * 解析AI响应
     */
    private List<DisasterInfo> parseResponse(String response) {
        List<DisasterInfo> disasters = new ArrayList<>();

        try {
            String jsonStr = extractJson(response);
            JSONObject json = JSON.parseObject(jsonStr);
            JSONArray array = json.getJSONArray("disasters");

            if (array == null) {
                return disasters;
            }

            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                DisasterInfo d = new DisasterInfo();

                String type = item.getString("type");
                if (type == null || type.isEmpty()) {
                    type = inferDisasterType(item.getInteger("weatherCode"));
                }
                d.setType(type);
                d.setWeatherCode(item.getInteger("weatherCode"));
                d.setStartHour(item.getInteger("startHour"));
                d.setEndHour(item.getInteger("endHour"));
                d.setLevel(item.getInteger("level"));
                d.setDescription(item.getString("description"));

                disasters.add(d);
            }

        } catch (Exception e) {
            log.error("[WeatherAnalysisAndAssessmentNode] 解析响应失败: {}", response, e);
        }

        return disasters;
    }

    /**
     * 从响应中提取JSON
     */
    private String extractJson(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        return response.trim();
    }

    /**
     * 根据天气码推断灾害类型
     */
    private String inferDisasterType(Integer weatherCode) {
        if (weatherCode == null) return "未知";
        return switch (weatherCode) {
            case 1, 7, 8 -> "晴好天气";
            case 15 -> "雷阵雨";
            case 33 -> "雾";
            case 46 -> "小雨";
            case 47 -> "中雨";
            case 48 -> "大雨";
            case 49 -> "暴雨";
            case 75 -> "霾/沙尘";
            default -> "未知天气";
        };
    }
}
