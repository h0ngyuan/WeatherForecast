package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.base.AIClient;
import com.wf.agent.constants.DisasterPromptProvider;
import com.wf.agent.entity.DisasterInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 灾害等级评估 Node
 *
 * 职责：
 * 使用 AI 评审每个灾害，判定等级和是否总是提醒
 *
 * 输入 State:
 *   - location: String (地区名称)
 *   - weatherCodes: List<Integer> (24小时天气码)
 *   - preliminaryDisasters: List<DisasterInfo> (无等级的灾害列表)
 *
 * 输出 State:
 *   - confirmedDisasters: List<DisasterInfo> (带等级的灾害列表)
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
public class DisasterLevelAssessmentNode implements NodeAction {

    private final AIClient aiClient;
    private final DisasterPromptProvider promptProvider;

    public DisasterLevelAssessmentNode(AIClient aiClient, DisasterPromptProvider promptProvider) {
        this.aiClient = aiClient;
        this.promptProvider = promptProvider;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String location = state.value("location", "unknown");
        @SuppressWarnings("unchecked")
        List<Integer> weatherCodes = state.value("weatherCodes", List.of());
        @SuppressWarnings("unchecked")
        List<DisasterInfo> preliminaryDisasters = state.value("preliminaryDisasters", List.of());

        log.info("[DisasterLevelAssessmentNode] 评估 {} 个灾害的等级", preliminaryDisasters.size());

        List<DisasterInfo> confirmedDisasters = new ArrayList<>();

        for (DisasterInfo disaster : preliminaryDisasters) {
            log.info("[DisasterLevelAssessmentNode] 评估灾害: {}", disaster.getType());

            // 使用 DisasterPromptProvider 构建评审 Prompt
            String prompt = promptProvider.getDisasterReviewPrompt(location, weatherCodes, disaster);

            String response = aiClient.chatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            AssessmentResult result = parseAssessmentResponse(response);

            disaster.setLevel(result.level());
            disaster.setDescription(result.reason());
            confirmedDisasters.add(disaster);

            log.info("[DisasterLevelAssessmentNode] {} 判定为 {} 级: {}",
                    disaster.getType(), result.level(), result.reason());
        }

        return Map.of("confirmedDisasters", confirmedDisasters);
    }

    /**
     * 解析 AI 响应
     */
    private AssessmentResult parseAssessmentResponse(String response) {
        try {
            String jsonStr = extractJson(response);
            JSONObject json = JSON.parseObject(jsonStr);

            boolean valid = json.getBooleanValue("valid", true);
            int level = json.getIntValue("level", 3);
            String reason = json.getString("reason");

            if (reason == null || reason.isEmpty()) {
                reason = "未获取到详细说明";
            }

            // 如果评审不通过，标记为3级（轻微）
            if (!valid) {
                level = 3;
            }

            return new AssessmentResult(level, reason);

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
    private record AssessmentResult(int level, String reason) {}
}
