package com.wf.agent.map.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.graph.node.AblationRunnerNode;
import com.wf.agent.map.graph.node.CaseLoaderNode;
import com.wf.agent.map.graph.node.ResultComparatorNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.Map.entry;

/**
 * EvaluationGraph评测图配置
 * 职责：历史案例回放 + 消融对比测试
 */
@Slf4j
@Configuration
public class EvaluationGraph {

    @Autowired
    private CaseLoaderNode caseLoaderNode;

    @Autowired
    private ResultComparatorNode resultComparatorNode;

    @Autowired
    private AblationRunnerNode ablationRunnerNode;

    @Autowired
    @Qualifier("mapInsightCompiledGraph")
    private CompiledGraph mapInsightGraph;

    /**
     * 回放子图：加载案例 -> 运行MapInsightGraph -> 对比结果
     */
    @Bean("replayGraph")
    public CompiledGraph replayGraph() throws Exception {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           ReplayGraph 评测子图构建开始                        ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry("caseId", new ReplaceStrategy()),
            entry("caseLimit", new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_CASE_LIST, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_CURRENT_CASE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_LOCATION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_RADIUS_KM, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_DATE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_QUERY, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_NEARBY_CITIES, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_CONFIDENCE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_CONFIDENCE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_CONFIDENCE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_ARBITRATION_RESULT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_FINAL_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_FINAL_CONFIDENCE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_ARBITRATION_REASON, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_FINAL_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_ACCURACY, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_LEAD_TIME, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_CONSISTENCY, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_RESPONSE_DELAY, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_EXECUTION_START_TIME, new ReplaceStrategy())
        );

        StateGraph graph = new StateGraph(keyStrategy);

        graph.addNode("loadCase", AsyncNodeAction.node_async(caseLoaderNode));
        graph.addNode("runAnalysis", mapInsightGraph);
        graph.addNode("compareResult", AsyncNodeAction.node_async(resultComparatorNode));

        graph.addEdge(START, "loadCase");
        graph.addEdge("loadCase", "runAnalysis");
        graph.addEdge("runAnalysis", "compareResult");
        graph.addEdge("compareResult", END);

        CompiledGraph compiledGraph = graph.compile();

        log.info("║  ReplayGraph 构建完成：START -> loadCase -> runAnalysis -> compareResult -> END");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        return compiledGraph;
    }

    /**
     * 消融子图：运行消融配置 -> 对比差异
     */
    @Bean("ablationGraph")
    public CompiledGraph ablationGraph() throws Exception {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           AblationGraph 消融子图构建开始                      ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry(MapGraphConstants.KEY_LOCATION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_RADIUS_KM, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_DATE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_QUERY, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_EXCLUDE_AGENTS, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_NEARBY_CITIES, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_CONFIDENCE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_CONFIDENCE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_CONFIDENCE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_ARBITRATION_RESULT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_FINAL_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_FINAL_CONFIDENCE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_ARBITRATION_REASON, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_FINAL_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_ABLATION_REPORT, new ReplaceStrategy())
        );

        StateGraph graph = new StateGraph(keyStrategy);

        graph.addNode("runAblation", AsyncNodeAction.node_async(ablationRunnerNode));

        graph.addEdge(START, "runAblation");
        graph.addEdge("runAblation", END);

        CompiledGraph compiledGraph = graph.compile();

        log.info("║  AblationGraph 构建完成：START -> runAblation -> END");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        return compiledGraph;
    }
}
