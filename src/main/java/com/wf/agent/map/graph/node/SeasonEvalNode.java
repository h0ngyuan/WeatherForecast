package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wf.agent.map.constants.MapGraphConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 季节评估子图 - 季节风险评估节点
 * 职责：根据历史统计计算季节性风险基线
 */
@Component
@Slf4j
public class SeasonEvalNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String season = state.value(MapGraphConstants.KEY_SEASON, "");
        String historyStatsStr = state.value(MapGraphConstants.KEY_HISTORY_STATS, "{}");
        JSONObject historyStats = JSON.parseObject(historyStatsStr);
        
        int totalCases = historyStats.getIntValue("totalCases");
        JSONObject eventTypeCounts = historyStats.getJSONObject("eventTypeCounts");
        
        log.info("[SeasonEvalNode] 评估 {} 季节风险基线，历史案例数：{}", season, totalCases);

        // 基于历史案例数评估季节性风险基线
        int riskBase = calculateSeasonRisk(season, totalCases, eventTypeCounts);
        
        log.info("[SeasonEvalNode] 季节风险基线：{}", riskBase);
        
        return Map.of(MapGraphConstants.KEY_SEASON_RISK_BASE, riskBase);
    }

    /**
     * 计算季节性风险基线
     * 基于季节特征和历史案例数
     */
    private int calculateSeasonRisk(String season, int totalCases, JSONObject eventTypeCounts) {
        if (totalCases == 0) return 0;

        // 不同季节的风险特征
        int baseRisk = 0;
        switch (season) {
            case "春季":
                // 春季：大风、沙尘
                baseRisk = 1;
                break;
            case "夏季":
                // 夏季：暴雨、雷电、台风（高风险）
                baseRisk = 2;
                break;
            case "秋季":
                // 秋季：相对平稳
                baseRisk = 1;
                break;
            case "冬季":
                // 冬季：降雪、冰冻
                baseRisk = 1;
                break;
        }

        // 根据历史案例数调整
        if (totalCases > 20) baseRisk = Math.min(3, baseRisk + 1);
        if (totalCases > 50) baseRisk = Math.min(3, baseRisk + 1);

        return baseRisk;
    }
}
