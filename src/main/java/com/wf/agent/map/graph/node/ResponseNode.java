package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wf.agent.map.constants.MapGraphConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 父图 - 响应生成节点
 * 职责：综合仲裁结果和3个Agent报告，生成最终响应
 */
@Component
@Slf4j
public class ResponseNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Integer finalRiskLevel = state.value(MapGraphConstants.KEY_FINAL_RISK_LEVEL, 0);
        Double finalConfidence = state.value(MapGraphConstants.KEY_FINAL_CONFIDENCE, 0.0);
        String arbitrationReason = state.value(MapGraphConstants.KEY_ARBITRATION_REASON, "");
        String location = state.value(MapGraphConstants.KEY_LOCATION, "未知");
        String query = state.value(MapGraphConstants.KEY_QUERY, "");
        String dateStr = state.value(MapGraphConstants.KEY_DATE, LocalDateTime.now().toString());

        log.info("[ResponseNode] 生成最终响应，地点：{}，风险等级：{}", location, finalRiskLevel);

        Map<String, Object> finalReport = new HashMap<>();
        finalReport.put("location", location);
        finalReport.put("query", query);
        finalReport.put("analysisDate", dateStr);
        finalReport.put("riskLevel", finalRiskLevel);
        finalReport.put("confidence", finalConfidence);
        finalReport.put("arbitrationReason", arbitrationReason);
        
        // 风险等级说明
        finalReport.put("riskLevelDesc", getRiskLevelDesc(finalRiskLevel));
        
        // 建议
        finalReport.put("suggestions", generateSuggestions(finalRiskLevel));

        // 收集各Agent报告
        List<Object> agentReports = new ArrayList<>();
        String trendReport = state.value(MapGraphConstants.KEY_TREND_REPORT, "");
        String seasonReport = state.value(MapGraphConstants.KEY_SEASON_REPORT, "");
        String impactReport = state.value(MapGraphConstants.KEY_IMPACT_REPORT, "");
        
        if (!trendReport.isEmpty()) {
            try { agentReports.add(JSON.parseObject(trendReport)); } catch (Exception ignored) {}
        }
        if (!seasonReport.isEmpty()) {
            try { agentReports.add(JSON.parseObject(seasonReport)); } catch (Exception ignored) {}
        }
        if (!impactReport.isEmpty()) {
            try { agentReports.add(JSON.parseObject(impactReport)); } catch (Exception ignored) {}
        }
        finalReport.put("agentReports", agentReports);

        // 生成结论文本
        String conclusion = generateConclusion(location, finalRiskLevel, arbitrationReason);
        finalReport.put("conclusion", conclusion);

        log.info("[ResponseNode] 响应生成完成：{}", conclusion);
        
        return Map.of(MapGraphConstants.KEY_FINAL_REPORT, JSON.toJSONString(finalReport));
    }

    private String getRiskLevelDesc(int level) {
        switch (level) {
            case 0: return "无风险";
            case 1: return "低风险 - 注意关注天气变化";
            case 2: return "中风险 - 建议做好防范准备";
            case 3: return "高风险 - 需立即采取应急措施";
            default: return "未知";
        }
    }

    private List<String> generateSuggestions(int riskLevel) {
        List<String> suggestions = new ArrayList<>();
        switch (riskLevel) {
            case 0:
                suggestions.add("天气状况良好，无需特别防范");
                break;
            case 1:
                suggestions.add("关注天气变化趋势");
                suggestions.add("建议携带雨具");
                break;
            case 2:
                suggestions.add("减少不必要的外出");
                suggestions.add("检查门窗和排水系统");
                suggestions.add("关注官方预警信息");
                break;
            case 3:
                suggestions.add("立即采取应急措施");
                suggestions.add("避免前往危险区域");
                suggestions.add("准备应急物资");
                suggestions.add("听从政府应急指挥");
                break;
        }
        return suggestions;
    }

    private String generateConclusion(String location, int riskLevel, String arbitrationReason) {
        if (riskLevel == 0) {
            return String.format("%s当前天气风险较低，无显著灾害威胁。%s", location, arbitrationReason);
        }
        return String.format("%s当前风险等级为%d级（%s），%s", 
                location, riskLevel, getRiskLevelDesc(riskLevel), arbitrationReason);
    }
}
