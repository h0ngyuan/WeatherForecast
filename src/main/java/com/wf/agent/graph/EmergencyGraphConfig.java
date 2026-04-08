package com.wf.agent.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.wf.agent.graph.node.AlertTextGenerationNode;
import com.wf.agent.graph.node.DisasterAssessmentNode;
import com.wf.agent.graph.node.DisasterLevelAssessmentNode;
import com.wf.agent.graph.node.WeatherPredictionNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static java.util.Map.entry;

/**
 * 紧急响应工作流配置
 *
 * 编排四个 Node 组成完整的紧急响应流程：
 * 1. WeatherPredictionNode: MCP查询24小时天气码
 * 2. DisasterAssessmentNode: AI识别灾害
 * 3. DisasterLevelAssessmentNode: AI+Skill评判等级
 * 4. AlertTextGenerationNode: 生成预警文本
 *
 * Workflow:
 * START -> weatherPrediction -> disasterAssessment -> disasterLevelAssessment -> alertTextGeneration -> END
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class EmergencyGraphConfig {

    @Bean("emergencyResponseGraph")
    public CompiledGraph emergencyResponseGraph(
            WeatherPredictionNode weatherPredictionNode,
            DisasterAssessmentNode disasterAssessmentNode,
            DisasterLevelAssessmentNode disasterLevelAssessmentNode,
            AlertTextGenerationNode alertTextGenerationNode) throws Exception {

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           EmergencyResponseGraph 工作流构建开始               ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        log.info("【步骤1】配置状态键策略 (KeyStrategy)");
        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry("location", new ReplaceStrategy()),
            entry("latitude", new ReplaceStrategy()),
            entry("longitude", new ReplaceStrategy()),
            entry("weatherCodes", new ReplaceStrategy()),
            entry("preliminaryDisasters", new ReplaceStrategy()),
            entry("confirmedDisasters", new ReplaceStrategy()),
            entry("alertText", new ReplaceStrategy())
        );
        log.info("  └─ 已注册 {} 个状态键", 7);

        StateGraph graph = new StateGraph(keyStrategy);

        log.info("【步骤2】注册工作流节点");
        graph.addNode("weatherPrediction", AsyncNodeAction.node_async(weatherPredictionNode));
        graph.addNode("disasterAssessment", AsyncNodeAction.node_async(disasterAssessmentNode));
        graph.addNode("disasterLevelAssessment", AsyncNodeAction.node_async(disasterLevelAssessmentNode));
        graph.addNode("alertTextGeneration", AsyncNodeAction.node_async(alertTextGenerationNode));
        log.info("  ├─ [weatherPrediction]      MCP查询24小时天气码");
        log.info("  ├─ [disasterAssessment]     AI识别灾害");
        log.info("  ├─ [disasterLevelAssessment] AI+Skill评判等级");
        log.info("  └─ [alertTextGeneration]    生成预警文本");

        log.info("【步骤3】配置工作流边连接");
        graph.addEdge(StateGraph.START, "weatherPrediction");
        log.info("  └─ START -> weatherPrediction");

        graph.addEdge("weatherPrediction", "disasterAssessment");
        log.info("  └─ weatherPrediction -> disasterAssessment");

        graph.addEdge("disasterAssessment", "disasterLevelAssessment");
        log.info("  └─ disasterAssessment -> disasterLevelAssessment");

        graph.addEdge("disasterLevelAssessment", "alertTextGeneration");
        log.info("  └─ disasterLevelAssessment -> alertTextGeneration");

        graph.addEdge("alertTextGeneration", StateGraph.END);
        log.info("  └─ alertTextGeneration -> END");

        CompiledGraph compiledGraph = graph.compile();

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           EmergencyResponseGraph 工作流构建完成               ║");
        log.info("║                                                              ║");
        log.info("║  流程图:                                                     ║");
        log.info("║  START -> weatherPrediction -> disasterAssessment            ║");
        log.info("║              -> disasterLevelAssessment                      ║");
        log.info("║              -> alertTextGeneration -> END                   ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        return compiledGraph;
    }
}
