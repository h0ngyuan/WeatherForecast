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
 * 趋势分析子图 - 趋势报告生成节点
 * 职责：生成结构化趋势分析报告
 */
@Component
@Slf4j
public class TrendReportNode implements NodeAction {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        String direction = state.value(MapGraphConstants.KEY_TREND_DIRECTION, "未知");
        Integer severity = state.value(MapGraphConstants.KEY_TREND_SEVERITY, 0);
        List<Object> nearbyCitiesRaw = state.value(MapGraphConstants.KEY_NEARBY_CITIES, new ArrayList<>());
        
        // 统计灾害城市
        int disasterCount = 0;
        int maxLevel = 0;
        for (Object cityRaw : nearbyCitiesRaw) {
            try {
                Map<String, Object> city = (Map<String, Object>) cityRaw;
                Boolean hasDisaster = (Boolean) city.get("hasDisaster");
                if (Boolean.TRUE.equals(hasDisaster)) {
                    disasterCount++;
                    Integer level = (Integer) city.get("maxDisasterLevel");
                    if (level != null) {
                        maxLevel = Math.max(maxLevel, level);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 构建报告
        AgentReport report = new AgentReport();
        report.setAgentName("TrendAgent");
        report.setReportType("TREND");
        report.setExecutionTimeMs(System.currentTimeMillis());
        
        String conclusion = String.format("发现%d个城市有灾害，最高风险等级%d级，灾害呈%s趋势",
                disasterCount, maxLevel, direction);
        report.setConclusion(conclusion);
        report.setRiskLevel(severity);
        
        // 置信度：基于数据量
        double confidence = Math.min(0.9, 0.5 + nearbyCitiesRaw.size() * 0.02);
        report.setConfidence(confidence);
        
        // 证据列表
        List<String> evidence = new ArrayList<>();
        evidence.add(String.format("监测城市总数：%d", nearbyCitiesRaw.size()));
        evidence.add(String.format("灾害城市数量：%d", disasterCount));
        evidence.add(String.format("最高风险等级：%d级", maxLevel));
        evidence.add(String.format("传播趋势：%s", direction));
        report.setEvidence(evidence);
        
        // 详细信息
        Map<String, Object> details = new HashMap<>();
        details.put("totalCities", nearbyCitiesRaw.size());
        details.put("disasterCities", disasterCount);
        details.put("maxLevel", maxLevel);
        details.put("trendDirection", direction);
        details.put("trendSeverity", severity);
        report.setDetails(details);

        // 计算风险等级
        int riskLevel = calculateRiskLevel(severity, disasterCount);
        
        log.info("[TrendReportNode] 生成报告：风险等级{}，置信度{}", riskLevel, confidence);
        
        return Map.of(
            MapGraphConstants.KEY_TREND_REPORT, JSON.toJSONString(report),
            MapGraphConstants.KEY_TREND_RISK_LEVEL, riskLevel,
            MapGraphConstants.KEY_TREND_CONFIDENCE, confidence
        );
    }

    /**
     * 根据严重程度和灾害城市数计算最终风险等级
     */
    private int calculateRiskLevel(int severity, int disasterCount) {
        if (severity == 0) return 0;
        if (severity >= 3) return Math.min(3, severity);
        if (severity >= 2 && disasterCount >= 2) return Math.min(3, severity);
        return Math.max(1, severity);
    }
}
