package com.wf.agent.map.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.graph.node.SeasonEvalNode;
import com.wf.agent.map.graph.node.SeasonHistoryNode;
import com.wf.agent.map.graph.node.SeasonInfoNode;
import com.wf.agent.map.graph.node.SeasonReportNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.Map.entry;

/**
 * SeasonAgent子图配置
 * 职责：结合季节特征，评估城市风险基线
 */
@Slf4j
@Configuration
public class SeasonAgentGraph {

    @Autowired
    private SeasonInfoNode seasonInfoNode;

    @Autowired
    private SeasonHistoryNode seasonHistoryNode;

    @Autowired
    private SeasonEvalNode seasonEvalNode;

    @Autowired
    private SeasonReportNode seasonReportNode;

    @Bean("seasonAgent")
    public CompiledGraph seasonAgent() throws Exception {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           SeasonAgent 子图构建开始                            ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry(MapGraphConstants.KEY_LOCATION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_DATE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_MONTH, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_HISTORY_STATS, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_RISK_BASE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_CONFIDENCE, new ReplaceStrategy())
        );

        StateGraph graph = new StateGraph(keyStrategy);

        graph.addNode("getSeasonInfo", AsyncNodeAction.node_async(seasonInfoNode));
        graph.addNode("queryHistoryStats", AsyncNodeAction.node_async(seasonHistoryNode));
        graph.addNode("evaluateSeasonRisk", AsyncNodeAction.node_async(seasonEvalNode));
        graph.addNode("generateSeasonReport", AsyncNodeAction.node_async(seasonReportNode));

        graph.addEdge(START, "getSeasonInfo");
        graph.addEdge("getSeasonInfo", "queryHistoryStats");
        graph.addEdge("queryHistoryStats", "evaluateSeasonRisk");
        graph.addEdge("evaluateSeasonRisk", "generateSeasonReport");
        graph.addEdge("generateSeasonReport", END);

        CompiledGraph compiledGraph = graph.compile();

        log.info("║  SeasonAgent 子图构建完成: START -> getSeasonInfo -> queryHistoryStats -> evaluateSeasonRisk -> generateSeasonReport -> END");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        return compiledGraph;
    }
}
