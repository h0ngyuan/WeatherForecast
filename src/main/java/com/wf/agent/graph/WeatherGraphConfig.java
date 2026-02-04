package com.wf.agent.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.agent.graph.edge.WeatherGraphEdgeActions;
import com.wf.agent.graph.node.WeatherAnswerGenerateNode;
import com.wf.agent.graph.node.WeatherRelevanceJudgeNode;
import com.wf.agent.graph.node.WeatherSemanticTransformNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static java.util.Map.entry;

@Slf4j
@Configuration
public class WeatherGraphConfig {

    @Bean("weatherGraph")
    public CompiledGraph weatherGraph(
            WeatherRelevanceJudgeNode judgeNode,
            WeatherSemanticTransformNode transformNode,
            WeatherAnswerGenerateNode generateNode) throws GraphStateException {

        log.info("========== 开始构建 WeatherGraph ==========");

        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry(WeatherGraphConstants.KEY_QUESTION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_RELEVANCE_SCORE, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_LOCATION_INFO, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_ANSWER, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_QUALITY_SCORE, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_LOOP_COUNT, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_NEXT_ACTION, new ReplaceStrategy())
        );

        StateGraph graph = new StateGraph(keyStrategy);

        graph.addNode("judge", AsyncNodeAction.node_async(judgeNode));
        graph.addNode("transform", AsyncNodeAction.node_async(transformNode));
        graph.addNode("generate", AsyncNodeAction.node_async(generateNode));

        log.info("添加节点: judge, transform, generate");

        graph.addEdge(StateGraph.START, "judge");
        log.info("添加边: START -> judge");

        graph.addConditionalEdges("judge",
            AsyncEdgeAction.edge_async(WeatherGraphEdgeActions::judgeEdge),
            Map.of("relevant", "transform", "irrelevant", StateGraph.END)
        );
        log.info("添加条件边: judge -> transform/END");

        graph.addEdge("transform", "generate");
        log.info("添加边: transform -> generate");

        graph.addConditionalEdges("generate",
            AsyncEdgeAction.edge_async(WeatherGraphEdgeActions::generateEdge),
            Map.of("loop", "generate", "break", StateGraph.END)
        );
        log.info("添加条件边: generate -> generate/END");

        CompiledGraph compiledGraph = graph.compile();
        log.info("========== WeatherGraph 构建完成 ==========");

        return compiledGraph;
    }
}