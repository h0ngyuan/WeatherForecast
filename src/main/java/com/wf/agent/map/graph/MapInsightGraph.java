package com.wf.agent.map.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.graph.node.ArbitrationNode;
import com.wf.agent.map.graph.node.ResponseNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.Map.entry;

/**
 * MapInsightGraph父图配置 - Coordinator/Harness
 * 职责：编排3个子图（TrendAgent、SeasonAgent、ImpactAgent），收集结果，仲裁冲突，生成响应
 */
@Slf4j
@Configuration
public class MapInsightGraph {

    @Autowired
    @Qualifier("trendAgent")
    private CompiledGraph trendAgent;

    @Autowired
    @Qualifier("seasonAgent")
    private CompiledGraph seasonAgent;

    @Autowired
    @Qualifier("impactAgent")
    private CompiledGraph impactAgent;

    @Autowired
    private ArbitrationNode arbitrationNode;

    @Autowired
    private ResponseNode responseNode;

    @Bean("mapInsightCompiledGraph")
    public CompiledGraph mapInsightGraph() throws Exception {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║         MapInsightGraph 父图构建开始 (Coordinator)           ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            // 输入
            entry(MapGraphConstants.KEY_LOCATION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_RADIUS_KM, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_DATE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_QUERY, new ReplaceStrategy()),
            
            // TrendAgent输出
            entry(MapGraphConstants.KEY_NEARBY_CITIES, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_DIRECTION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_SEVERITY, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_TREND_CONFIDENCE, new ReplaceStrategy()),
            
            // SeasonAgent输出
            entry(MapGraphConstants.KEY_SEASON, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_MONTH, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_HISTORY_STATS, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_RISK_BASE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_SEASON_CONFIDENCE, new ReplaceStrategy()),
            
            // ImpactAgent输出
            entry(MapGraphConstants.KEY_DISASTER_CITIES, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_SCORE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_DIRECTION, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_REPORT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_IMPACT_CONFIDENCE, new ReplaceStrategy()),
            
            // 仲裁输出
            entry(MapGraphConstants.KEY_ARBITRATION_RESULT, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_FINAL_RISK_LEVEL, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_FINAL_CONFIDENCE, new ReplaceStrategy()),
            entry(MapGraphConstants.KEY_ARBITRATION_REASON, new ReplaceStrategy()),
            
            // 最终输出
            entry(MapGraphConstants.KEY_FINAL_REPORT, new ReplaceStrategy())
        );

        StateGraph graph = new StateGraph(keyStrategy);

        // 注册3个子图作为节点（通过NodeAction包装CompiledGraph）
        graph.addNode("trendAgent", AsyncNodeAction.node_async(state -> invokeSubGraph(trendAgent, state)));
        graph.addNode("seasonAgent", AsyncNodeAction.node_async(state -> invokeSubGraph(seasonAgent, state)));
        graph.addNode("impactAgent", AsyncNodeAction.node_async(state -> invokeSubGraph(impactAgent, state)));
        
        // 注册仲裁和响应节点
        graph.addNode("arbitration", AsyncNodeAction.node_async(arbitrationNode));
        graph.addNode("generateResp", AsyncNodeAction.node_async(responseNode));

        // START并行到3个子图
        graph.addEdge(START, "trendAgent");
        graph.addEdge(START, "seasonAgent");
        graph.addEdge(START, "impactAgent");
        
        // 3个子图汇总到仲裁
        graph.addEdge("trendAgent", "arbitration");
        graph.addEdge("seasonAgent", "arbitration");
        graph.addEdge("impactAgent", "arbitration");
        
        // 仲裁到响应生成
        graph.addEdge("arbitration", "generateResp");
        
        // 响应到END
        graph.addEdge("generateResp", END);

        CompiledGraph compiledGraph = graph.compile();

        log.info("║  MapInsightGraph 父图构建完成：                              ║");
        log.info("║  START ──┬──> trendAgent (子图)                              ║");
        log.info("║          ├──> seasonAgent (子图)    ← 并行执行               ║");
        log.info("║          └──> impactAgent (子图)                             ║");
        log.info("║                  │                                            ║");
        log.info("║                  ▼                                            ║");
        log.info("║          ┌──────────────┐                                    ║");
        log.info("║          │ arbitration   │ ← 冲突仲裁                        ║");
        log.info("║          └──────┬───────┘                                    ║");
        log.info("║                 ▼                                             ║");
        log.info("║          ┌──────────────┐                                    ║");
        log.info("║          │ generateResp  │ ← 响应生成                        ║");
        log.info("║          └──────┬───────┘                                    ║");
        log.info("║                 ▼                                             ║");
        log.info("║                END                                            ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        return compiledGraph;
    }

    private Map<String, Object> invokeSubGraph(CompiledGraph subGraph, OverAllState state) {
        Optional<OverAllState> result = subGraph.invoke(state.data(), RunnableConfig.builder().build());
        if (result.isPresent()) {
            return result.get().data();
        }
        return Map.of();
    }
}
