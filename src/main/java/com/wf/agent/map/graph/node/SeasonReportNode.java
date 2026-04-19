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
 * 季节评估子图 - 季节报告生成节点
 * 职责：生成结构化季节评估报告
 */
@Component
@Slf4j
public class SeasonReportNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String season = state.value(MapGraphConstants.KEY_SEASON, "");
        String historyStatsStr = state.value(MapGraphConstants.KEY_HISTORY_STATS, "{}");
        Integer riskBase = state.value(MapGraphConstants.KEY_SEASON_RISK_BASE, 0);
        String dateStr = state.value(MapGraphConstants.KEY_DATE, "");
        
        log.info("[SeasonReportNode] 生成季节报告：{} 风险基线：{}", season, riskBase);

        AgentReport report = new AgentReport();
        report.setAgentName("SeasonAgent");
        report.setReportType("SEASON");
        report.setExecutionTimeMs(System.currentTimeMillis());

        String conclusion = String.format("%s属于季节性正常风险范围，历史同期灾害频发，风险等级%d级，建议保持关注",
                season, riskBase);
        report.setConclusion(conclusion);
        report.setRiskLevel(riskBase);

        double confidence = 0.6 + (riskBase > 0 ? 0.1 : 0);
        report.setConfidence(confidence);

        List<String> evidence = new ArrayList<>();
        evidence.add(String.format("当前季节：%s", season));
        evidence.add(String.format("历史同期风险基线：%d级", riskBase));
        evidence.add(String.format("季节性特征：%s", getSeasonFeature(season)));
        report.setEvidence(evidence);

        Map<String, Object> details = new HashMap<>();
        details.put("season", season);
        details.put("date", dateStr);
        details.put("historyStats", historyStatsStr);
        details.put("riskBase", riskBase);
        details.put("seasonFeature", getSeasonFeature(season));
        report.setDetails(details);

        String seasonTip = getSeasonTip(season);
        details.put("seasonTip", seasonTip);

        return Map.of(
            MapGraphConstants.KEY_SEASON_REPORT, JSON.toJSONString(report),
            MapGraphConstants.KEY_SEASON_RISK_LEVEL, riskBase,
            MapGraphConstants.KEY_SEASON_CONFIDENCE, confidence
        );
    }

    private String getSeasonFeature(String season) {
        switch (season) {
            case "春季": return "大风、沙尘天气频发，气温波动较大";
            case "夏季": return "暴雨、雷电、台风等强对流天气多发";
            case "秋季": return "天气相对平稳，偶有秋雨";
            case "冬季": return "降雪、冰冻、低温等灾害风险增加";
            default: return "未知季节特征";
        }
    }

    private String getSeasonTip(String season) {
        switch (season) {
            case "春季": return "春季风大，注意防风防火";
            case "夏季": return "汛期注意暴雨、雷电预警";
            case "秋季": return "秋季天气平稳，但仍需关注突发天气";
            case "冬季": return "冬季注意降雪、道路结冰预警";
            default: return "";
        }
    }
}
