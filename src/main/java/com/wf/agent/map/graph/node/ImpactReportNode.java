package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson.JSON;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.dto.AgentReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 影响分析子图 - 影响报告生成节点
 * 职责：生成结构化影响分析报告
 */
@Component
@Slf4j
public class ImpactReportNode implements NodeAction {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        Double impactScore = state.value(MapGraphConstants.KEY_IMPACT_SCORE, 0.0);
        String impactDirection = state.value(MapGraphConstants.KEY_IMPACT_DIRECTION, "未知");
        List<Object> disasterCitiesRaw = state.value(MapGraphConstants.KEY_DISASTER_CITIES, new ArrayList<>());
        
        log.info("[ImpactReportNode] 生成影响报告：影响分数{}，方向{}", impactScore, impactDirection);

        AgentReport report = new AgentReport();
        report.setAgentName("ImpactAgent");
        report.setReportType("IMPACT");
        report.setExecutionTimeMs(System.currentTimeMillis());

        int riskLevel = calculateImpactRiskLevel(impactScore, disasterCitiesRaw.size());
        String conclusion = String.format("周边%d个城市有灾害，影响来自%s方向，影响分数%.1f，风险等级%d级",
                disasterCitiesRaw.size(), impactDirection, impactScore, riskLevel);
        report.setConclusion(conclusion);
        report.setRiskLevel(riskLevel);

        double confidence = Math.min(0.95, 0.6 + disasterCitiesRaw.size() * 0.05);
        report.setConfidence(confidence);

        List<String> evidence = new ArrayList<>();
        evidence.add(String.format("灾害城市数量：%d", disasterCitiesRaw.size()));
        evidence.add(String.format("主要影响方向：%s", impactDirection));
        evidence.add(String.format("加权影响分数：%.1f", impactScore));
        for (Object raw : disasterCitiesRaw) {
            try {
                Map<String, Object> city = (Map<String, Object>) raw;
                evidence.add(String.format("- %s：距离%.0fkm，等级%d",
                        city.get("cityName"),
                        Double.parseDouble((String) city.get("displayIcon")),
                        city.get("maxDisasterLevel") != null ? ((Number) city.get("maxDisasterLevel")).intValue() : 1));
            } catch (Exception ignored) {}
        }
        report.setEvidence(evidence);

        Map<String, Object> details = new HashMap<>();
        details.put("impactScore", impactScore);
        details.put("impactDirection", impactDirection);
        details.put("disasterCityCount", disasterCitiesRaw.size());
        details.put("riskLevel", riskLevel);
        report.setDetails(details);

        return Map.of(
            MapGraphConstants.KEY_IMPACT_REPORT, JSON.toJSONString(report),
            MapGraphConstants.KEY_IMPACT_RISK_LEVEL, riskLevel,
            MapGraphConstants.KEY_IMPACT_CONFIDENCE, confidence
        );
    }

    /**
     * 根据影响分数计算风险等级
     */
    private int calculateImpactRiskLevel(double impactScore, int disasterCount) {
        if (impactScore == 0) return 0;
        if (impactScore >= 6 && disasterCount >= 3) return 3;
        if (impactScore >= 3 && disasterCount >= 2) return 2;
        return 1;
    }
}
