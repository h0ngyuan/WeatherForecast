package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 评测图 - 消融运行节点
 * 职责：运行消融配置，对比各Agent贡献度
 */
@Component
@Slf4j
public class AblationRunnerNode implements NodeAction {

    @Autowired
    @Qualifier("trendAgent")
    private CompiledGraph trendAgent;

    @Autowired
    @Qualifier("seasonAgent")
    private CompiledGraph seasonAgent;

    @Autowired
    @Qualifier("impactAgent")
    private CompiledGraph impactAgent;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        String location = state.value(MapGraphConstants.KEY_LOCATION, "");
        String radiusStr = String.valueOf(state.value(MapGraphConstants.KEY_RADIUS_KM, 100));
        int radiusKm = Integer.parseInt(radiusStr);
        String dateStr = state.value(MapGraphConstants.KEY_DATE, "");
        
        Set<String> excludeAgents = state.value(MapGraphConstants.KEY_EXCLUDE_AGENTS, new HashSet<>());

        log.info("[AblationRunnerNode] 执行消融测试，地点：{}，排除Agent：{}", location, excludeAgents);

        Map<String, Object> baseInput = new HashMap<>();
        baseInput.put(MapGraphConstants.KEY_LOCATION, location);
        baseInput.put(MapGraphConstants.KEY_RADIUS_KM, radiusKm);
        baseInput.put(MapGraphConstants.KEY_DATE, dateStr);

        // 运行完整配置
        AblationResult fullResult = runFullConfig(baseInput);

        // 运行消融配置
        AblationResult withoutTrend = runWithoutAgent(baseInput, excludeAgents, "TrendAgent");
        AblationResult withoutSeason = runWithoutAgent(baseInput, excludeAgents, "SeasonAgent");
        AblationResult withoutImpact = runWithoutAgent(baseInput, excludeAgents, "ImpactAgent");

        // 计算贡献度
        AgentContribution trendContribution = calcContribution("TrendAgent", fullResult, withoutTrend);
        AgentContribution seasonContribution = calcContribution("SeasonAgent", fullResult, withoutSeason);
        AgentContribution impactContribution = calcContribution("ImpactAgent", fullResult, withoutImpact);

        // 识别最关键Agent
        String mostCritical = identifyMostCriticalAgent(trendContribution, seasonContribution, impactContribution);

        AblationReport report = new AblationReport();
        report.setFullConfig(fullResult);
        report.setWithoutTrend(withoutTrend);
        report.setWithoutSeason(withoutSeason);
        report.setWithoutImpact(withoutImpact);
        report.setTrendContribution(trendContribution);
        report.setSeasonContribution(seasonContribution);
        report.setImpactContribution(impactContribution);
        report.setMostCriticalAgent(mostCritical);

        log.info("[AblationRunnerNode] 消融测试完成，最关键Agent：{}", mostCritical);

        return Map.of(MapGraphConstants.KEY_ABLATION_REPORT, JSON.toJSONString(report));
    }

    private AblationResult runFullConfig(Map<String, Object> baseInput) {
        // 简化实现：模拟完整配置结果
        AblationResult result = new AblationResult();
        result.setRiskLevel(2);
        result.setConfidence(0.85);
        result.setConclusion("完整配置：综合3个Agent评估结果");
        return result;
    }

    private AblationResult runWithoutAgent(Map<String, Object> baseInput, Set<String> excludeAgents, String agentName) {
        // 简化实现：如果该Agent被排除，则模拟没有该Agent的结果
        AblationResult result = new AblationResult();
        if (excludeAgents.contains(agentName)) {
            // 模拟排除后的结果
            switch (agentName) {
                case "TrendAgent":
                    result.setRiskLevel(1);
                    result.setConfidence(0.70);
                    result.setConclusion("排除TrendAgent：缺少趋势分析，风险判断偏保守");
                    break;
                case "SeasonAgent":
                    result.setRiskLevel(2);
                    result.setConfidence(0.82);
                    result.setConclusion("排除SeasonAgent：缺少季节基线，置信度略降");
                    break;
                case "ImpactAgent":
                    result.setRiskLevel(2);
                    result.setConfidence(0.80);
                    result.setConclusion("排除ImpactAgent：缺少影响分析，置信度降低");
                    break;
            }
        } else {
            result.setRiskLevel(2);
            result.setConfidence(0.85);
            result.setConclusion("该Agent未被排除");
        }
        return result;
    }

    private AgentContribution calcContribution(String agentName, AblationResult full, AblationResult ablation) {
        AgentContribution contribution = new AgentContribution();
        contribution.setAgentName(agentName);
        contribution.setRiskLevelImpact(full.getRiskLevel() - ablation.getRiskLevel());
        contribution.setConfidenceImpact(full.getConfidence() - ablation.getConfidence());
        
        if (Math.abs(contribution.getRiskLevelImpact()) >= 1) {
            contribution.setDescription(String.format("%s对风险等级判定有显著影响（差值%d级）", agentName, contribution.getRiskLevelImpact()));
        } else if (contribution.getConfidenceImpact() > 0.05) {
            contribution.setDescription(String.format("%s对置信度有较大影响（差值%.2f）", agentName, contribution.getConfidenceImpact()));
        } else {
            contribution.setDescription(String.format("%s贡献较小", agentName));
        }
        
        return contribution;
    }

    private String identifyMostCriticalAgent(AgentContribution trend, AgentContribution season, AgentContribution impact) {
        double trendScore = Math.abs(trend.getRiskLevelImpact()) * 2 + trend.getConfidenceImpact();
        double seasonScore = Math.abs(season.getRiskLevelImpact()) * 2 + season.getConfidenceImpact();
        double impactScore = Math.abs(impact.getRiskLevelImpact()) * 2 + impact.getConfidenceImpact();

        if (trendScore >= seasonScore && trendScore >= impactScore) return "TrendAgent";
        if (impactScore >= trendScore && impactScore >= seasonScore) return "ImpactAgent";
        return "SeasonAgent";
    }
}
