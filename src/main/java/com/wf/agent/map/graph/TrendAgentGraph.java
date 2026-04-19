package com.wf.agent.map.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.graph.node.TrendAnalysisNode;
import com.wf.agent.map.graph.node.TrendQueryNode;
import com.wf.agent.map.graph.node.TrendReportNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.Map.entry;

/**
 * TrendAgent子图配置
 * 职责：分析周边城市天气趋势，判断灾害传播方向
 */
@Slf4j
@Configuration
public class TrendAgentGraph {

    @Autowired
    private TrendQueryNode trendQueryNode;

    @Autowired
    private TrendAnalysisNode trendAnalysisNode;

    @Autowired
    private TrendReportNode trendReportNode;

    @Bean("trendAgent")
    public CompiledGraph trendAgent() throws Exception {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           TrendAgent 子图构建开始                             ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry(MapGraphConstants.KEY_LOCATION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_RADIUS_KM, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_DATE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_NEARBY_CITIES, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_DIRECTION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_SEVERITY, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_CONFIDENCE, new ReplaceStrategy())
        );

        StateGraph graph = new StateGraph(keyStrategy);

        graph.addNode("queryNearby", AsyncNodeAction.node_async(trendQueryNode));
        graph.addNode("analyzeTrend", AsyncNodeAction.node_async(trendAnalysisNode));
        graph.addNode("generateReport", AsyncNodeAction.node_async(trendReportNode));

        graph.addEdge(START, "queryNearby");
        graph.addEdge("queryNearby", "analyzeTrend");
        graph.addEdge("analyzeTrend", "generateReport");
        graph.addEdge("generateReport", END);

        CompiledGraph compiledGraph = graph.compile();

        log.info("║  TrendAgent 子图构建完成: START -> queryNearby -> analyzeTrend -> generateReport -> END");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        return compiledGraph;
    }
}
