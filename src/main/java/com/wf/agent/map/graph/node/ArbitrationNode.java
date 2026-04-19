package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.dto.AgentReport;
import com.wf.agent.map.dto.ArbitrationResult;
import com.wf.agent.map.dto.ConflictRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 父图 - 仲裁节点
 * 职责：收集3个Agent结果，仲裁冲突，生成最终决策
 */
@Component
@Slf4j
public class ArbitrationNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        long startTime = System.currentTimeMillis();
        
        int trendLevel = state.value(MapGraphConstants.KEY_TREND_RISK_LEVEL, 0);
        int seasonLevel = state.value(MapGraphConstants.KEY_SEASON_RISK_LEVEL, 0);
        int impactLevel = state.value(MapGraphConstants.KEY_IMPACT_RISK_LEVEL, 0);
        
        double trendConf = state.value(MapGraphConstants.KEY_TREND_CONFIDENCE, 0.0);
        double seasonConf = state.value(MapGraphConstants.KEY_SEASON_CONFIDENCE, 0.0);
        double impactConf = state.value(MapGraphConstants.KEY_IMPACT_CONFIDENCE, 0.0);

        String trendReportStr = state.value(MapGraphConstants.KEY_TREND_REPORT, "");
        String seasonReportStr = state.value(MapGraphConstants.KEY_SEASON_REPORT, "");
        String impactReportStr = state.value(MapGraphConstants.KEY_IMPACT_REPORT, "");

        log.info("[ArbitrationNode] Trend:{}级({}), Season:{}级({}), Impact:{}级({})",
                trendLevel, trendConf, seasonLevel, seasonConf, impactLevel, impactConf);

        // 构建Agent报告列表
        List<AgentReport> reports = new ArrayList<>();
        if (trendReportStr != null && !trendReportStr.isEmpty()) {
            try { reports.add(JSON.parseObject(trendReportStr, AgentReport.class)); } catch (Exception ignored) {}
        }
        if (seasonReportStr != null && !seasonReportStr.isEmpty()) {
            try { reports.add(JSON.parseObject(seasonReportStr, AgentReport.class)); } catch (Exception ignored) {}
        }
        if (impactReportStr != null && !impactReportStr.isEmpty()) {
            try { reports.add(JSON.parseObject(impactReportStr, AgentReport.class)); } catch (Exception ignored) {}
        }

        // 冲突检测
        Set<Integer> levels = new HashSet<>(Arrays.asList(trendLevel, seasonLevel, impactLevel));
        boolean hasConflict = levels.size() > 1;

        // 仲裁决策
        int finalLevel;
        String reason;
        List<ConflictRecord> conflicts = new ArrayList<>();

        if (!hasConflict) {
            finalLevel = trendLevel;
            reason = String.format("所有Agent意见一致：风险等级%d级", trendLevel);
            log.info("[ArbitrationNode] 无冲突，采用一致意见：{}级", finalLevel);
        } else {
            // 记录冲突
            conflicts = detectConflicts(trendLevel, seasonLevel, impactLevel,
                    trendConf, seasonConf, impactConf);
            
            // 采用最高风险等级（安全优先原则）
            finalLevel = Math.max(trendLevel, Math.max(seasonLevel, impactLevel));
            
            // 生成仲裁理由
            reason = generateArbitrationReason(trendLevel, seasonLevel, impactLevel,
                    trendConf, seasonConf, impactConf, finalLevel, conflicts);
            
            log.info("[ArbitrationNode] 检测到冲突，仲裁结果：{}级，理由：{}", finalLevel, reason);
        }

        // 加权置信度：ImpactAgent(动态分析)权重最高
        double finalConf = trendConf * 0.3 + seasonConf * 0.2 + impactConf * 0.5;
        finalConf = Math.min(0.95, finalConf);

        ArbitrationResult result = new ArbitrationResult();
        result.setFinalRiskLevel(finalLevel);
        result.setFinalConfidence(finalConf);
        result.setArbitrationReason(reason);
        result.setAgentReports(reports);
        result.setConflicts(conflicts);

        long executionTime = System.currentTimeMillis() - startTime;
        log.info("[ArbitrationNode] 仲裁完成，耗时{}ms", executionTime);

        return Map.of(
            MapGraphConstants.KEY_ARBITRATION_RESULT, JSON.toJSONString(result),
            MapGraphConstants.KEY_FINAL_RISK_LEVEL, finalLevel,
            MapGraphConstants.KEY_FINAL_CONFIDENCE, finalConf,
            MapGraphConstants.KEY_ARBITRATION_REASON, reason
        );
    }

    /**
     * 检测冲突
     */
    private List<ConflictRecord> detectConflicts(int trend, int season, int impact,
                                                   double trendConf, double seasonConf, double impactConf) {
        List<ConflictRecord> conflicts = new ArrayList<>();
        
        if (trend != season) {
            ConflictRecord cr = new ConflictRecord();
            cr.setConflictType("RISK_LEVEL_DIFF");
            cr.setAgentA("TrendAgent");
            cr.setAgentB("SeasonAgent");
            cr.setValueA(trend);
            cr.setValueB(season);
            cr.setResolution("采用较高值");
            conflicts.add(cr);
        }
        
        if (trend != impact) {
            ConflictRecord cr = new ConflictRecord();
            cr.setConflictType("RISK_LEVEL_DIFF");
            cr.setAgentA("TrendAgent");
            cr.setAgentB("ImpactAgent");
            cr.setValueA(trend);
            cr.setValueB(impact);
            cr.setResolution("采用较高值");
            conflicts.add(cr);
        }
        
        if (season != impact) {
            ConflictRecord cr = new ConflictRecord();
            cr.setConflictType("RISK_LEVEL_DIFF");
            cr.setAgentA("SeasonAgent");
            cr.setAgentB("ImpactAgent");
            cr.setValueA(season);
            cr.setValueB(impact);
            cr.setResolution("采用较高值");
            conflicts.add(cr);
        }
        
        return conflicts;
    }

    /**
     * 生成仲裁理由
     */
    private String generateArbitrationReason(int trend, int season, int impact,
                                              double trendConf, double seasonConf, double impactConf,
                                              int finalLevel, List<ConflictRecord> conflicts) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("TrendAgent评估%d级(%.2f)，", trend, trendConf));
        sb.append(String.format("SeasonAgent评估%d级(%.2f)，", season, seasonConf));
        sb.append(String.format("ImpactAgent评估%d级(%.2f)。", impact, impactConf));
        
        if (conflicts.size() > 1) {
            sb.append(String.format("3个Agent意见均不同，基于安全优先原则，采用最高风险等级%d级。", finalLevel));
        } else if (conflicts.size() == 1) {
            sb.append(String.format("2个Agent意见一致，采用多数意见%d级。", finalLevel));
        }
        
        // 优先级说明
        if (finalLevel == impact) {
            sb.append("ImpactAgent(动态影响分析)具有最高权重。");
        }
        
        return sb.toString();
    }
}
