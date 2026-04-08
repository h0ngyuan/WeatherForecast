package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
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
 * 灾害等级评估 Node
 *
 * 职责：
 * 使用 AI + Skill 规则评判每个灾害的风险等级
 *
 * 输入 State:
 *   - preliminaryDisasters: List<DisasterInfo> (无等级的灾害列表)
 *
 * 输出 State:
 *   - confirmedDisasters: List<DisasterInfo> (带等级的灾害列表)
 *
 * 核心逻辑：
 *   对每个灾害：
 *     1. 根据灾害类型读取对应的 Skill 规则
 *     2. 将 Skill 规则注入 Prompt
 *     3. AI 根据规则判定等级并给出解释
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
public class DisasterLevelAssessmentNode implements NodeAction {

    private final AIClient aiClient;
    private final SkillRegistry skillRegistry;

    public DisasterLevelAssessmentNode(AIClient aiClient, SkillRegistry skillRegistry) {
        this.aiClient = aiClient;
        this.skillRegistry = skillRegistry;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        @SuppressWarnings("unchecked")
        List<DisasterInfo> preliminaryDisasters = state.value("preliminaryDisasters", List.of());

        log.info("[DisasterLevelAssessmentNode] 评估 {} 个灾害的等级", preliminaryDisasters.size());

        List<DisasterInfo> confirmedDisasters = new ArrayList<>();

        for (DisasterInfo disaster : preliminaryDisasters) {
            log.info("[DisasterLevelAssessmentNode] 评估灾害: {}", disaster.getType());

            String skillRules = readSkillRules(disaster.getType());
            String prompt = buildAssessmentPrompt(disaster, skillRules);

            String response = aiClient.chatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            AssessmentResult result = parseAssessmentResponse(response);

            disaster.setLevel(result.level());
            disaster.setDescription(result.explanation());
            confirmedDisasters.add(disaster);

            log.info("[DisasterLevelAssessmentNode] {} 判定为 {} 级: {}",
                    disaster.getType(), result.level(), result.explanation());
        }

        return Map.of("confirmedDisasters", confirmedDisasters);
    }

    /**
     * 读取 Skill 规则
     */
    private String readSkillRules(String disasterType) {
        // 如果灾害类型为空，直接返回默认规则
        if (disasterType == null || disasterType.isEmpty()) {
            log.warn("[DisasterLevelAssessmentNode] 灾害类型为空，使用默认规则");
            return """
                默认判定规则：
                - 1级（严重）：对生命财产有直接威胁，需要全员预警
                - 2级（中等）：影响特定活动，需要提醒相关人员
                - 3级（轻微）：一般性提醒即可
                """;
        }
        
        String rules = skillRegistry.readSkillRulesByDisasterType(disasterType);
        if (rules == null || rules.isEmpty() || rules.contains("not found")) {
            return """
                默认判定规则：
                - 1级（严重）：对生命财产有直接威胁，需要全员预警
                - 2级（中等）：影响特定活动，需要提醒相关人员
                - 3级（轻微）：一般性提醒即可
                """;
        }
        return rules;
    }

    /**
     * 构建评估 Prompt
     */
    private String buildAssessmentPrompt(DisasterInfo disaster, String skillRules) {
        int durationHours = disaster.getEndHour() - disaster.getStartHour() + 1;

        return String.format("""
            你是一位灾害风险评估专家。请根据以下 Skill 规则，判定该灾害的风险等级。

            ## 灾害信息
            - 类型: %s
            - 天气码值: %d
            - 持续时间: %d小时（第%d小时到第%d小时）
            - 描述: %s

            ## Skill 判定规则
            %s

            ## 输出要求
            请严格按照以下 JSON 格式输出判定结果：
            {
              "level": 1或2或3,
              "explanation": "详细的风险描述和建议措施，说明为什么判定为这个等级"
            }

            注意：
            1. 必须严格遵循 Skill 规则中的判定逻辑
            2. explanation 要有可解释性，说明判定的依据
            3. 只输出 JSON，不要其他内容
            """,
                disaster.getType(),
                disaster.getWeatherCode(),
                durationHours,
                disaster.getStartHour(),
                disaster.getEndHour(),
                disaster.getDescription(),
                skillRules
        );
    }

    /**
     * 解析 AI 响应
     */
    private AssessmentResult parseAssessmentResponse(String response) {
        try {
            String jsonStr = extractJson(response);
            JSONObject json = JSON.parseObject(jsonStr);

            int level = json.getIntValue("level", 3);
            String explanation = json.getString("explanation");

            if (explanation == null || explanation.isEmpty()) {
                explanation = "未获取到详细说明";
            }

            return new AssessmentResult(level, explanation);

        } catch (Exception e) {
            log.error("[DisasterLevelAssessmentNode] 解析响应失败: {}", response, e);
            return new AssessmentResult(3, "解析失败，默认一般性提醒");
        }
    }

    /**
     * 从响应中提取 JSON
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
     * 评估结果记录
     */
    private record AssessmentResult(int level, String explanation) {}
}
