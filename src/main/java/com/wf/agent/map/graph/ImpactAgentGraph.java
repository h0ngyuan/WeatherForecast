package com.wf.agent.map.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.graph.node.DisasterQueryNode;
import com.wf.agent.map.graph.node.ImpactCalcNode;
import com.wf.agent.map.graph.node.ImpactReportNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.Map.entry;

/**
 * ImpactAgent子图配置
 * 职责：分析周边灾害对目标城市的直接影响
 */
@Slf4j
@Configuration
public class ImpactAgentGraph {

    @Autowired
    private DisasterQueryNode disasterQueryNode;

    @Autowired
    private ImpactCalcNode impactCalcNode;

    @Autowired
    private ImpactReportNode impactReportNode;

    @Bean("impactAgent")
    public CompiledGraph impactAgent() throws Exception {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           ImpactAgent 子图构建开始                            ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry(MapGraphConstants.KEY_LOCATION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_RADIUS_KM, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_DATE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_DISASTER_CITIES, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_SCORE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_DIRECTION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_CONFIDENCE, new ReplaceStrategy())
        );

        StateGraph graph = new StateGraph(keyStrategy);

        graph.addNode("findDisasterCities", AsyncNodeAction.node_async(disasterQueryNode));
        graph.addNode("calculateImpact", AsyncNodeAction.node_async(impactCalcNode));
        graph.addNode("generateImpactReport", AsyncNodeAction.node_async(impactReportNode));

        graph.addEdge(START, "findDisasterCities");
        graph.addEdge("findDisasterCities", "calculateImpact");
        graph.addEdge("calculateImpact", "generateImpactReport");
        graph.addEdge("generateImpactReport", END);

        CompiledGraph compiledGraph = graph.compile();

        log.info("║  ImpactAgent 子图构建完成: START -> findDisasterCities -> calculateImpact -> generateImpactReport -> END");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        return compiledGraph;
    }
}
