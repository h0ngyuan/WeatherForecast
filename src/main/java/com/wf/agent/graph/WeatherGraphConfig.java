package com.wf.agent.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.agent.graph.node.WeatherAnswerGenerateNode;
import com.wf.agent.graph.node.WeatherForecastNode;
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
            WeatherForecastNode forecastNode,
            WeatherAnswerGenerateNode generateNode) throws GraphStateException {

        log.info("========== 开始构建 WeatherGraph ==========");

        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry(WeatherGraphConstants.KEY_QUESTION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_RELEVANCE_SCORE, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_WEATHER_CODE_QUERY, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_FORECAST_RESULT, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_ANSWER, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_QUALITY_SCORE, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_LOOP_COUNT, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_NEXT_ACTION, new ReplaceStrategy())
        );

        StateGraph graph = new StateGraph(keyStrategy);

        graph.addNode("judge", AsyncNodeAction.node_async(judgeNode));
        graph.addNode("transform", AsyncNodeAction.node_async(transformNode));
        graph.addNode("forecast", AsyncNodeAction.node_async(forecastNode));
        graph.addNode("generate", AsyncNodeAction.node_async(generateNode));

        log.info("添加节点: judge, transform, forecast, generate");

        graph.addEdge(StateGraph.START, "judge");
        log.info("添加边: START -> judge");

        graph.addConditionalEdges("judge",
            AsyncEdgeAction.edge_async(state -> state.value(WeatherGraphConstants.KEY_RELEVANCE_SCORE, 0.0) >= WeatherGraphConstants.THRESHOLD_RELEVANCE ? "relevant" : "irrelevant"),
            Map.of("relevant", "transform", "irrelevant", StateGraph.END)
        );
        log.info("添加条件边: judge -> transform/END");

        graph.addEdge("transform", "forecast");
        log.info("添加边: transform -> forecast");

        graph.addEdge("forecast", "generate");
        log.info("添加边: forecast -> generate");

        graph.addConditionalEdges("generate",
            AsyncEdgeAction.edge_async(state->state.value(WeatherGraphConstants.KEY_NEXT_ACTION, "break")),
            Map.of("loop", "generate", "break", StateGraph.END)
        );
        log.info("添加条件边: generate -> generate/END");

        CompiledGraph compiledGraph = graph.compile();
        log.info("========== WeatherGraph 构建完成 ==========");

        return compiledGraph;
    }
}