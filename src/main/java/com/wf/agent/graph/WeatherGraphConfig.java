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
import com.wf.agent.graph.node.WeatherRelevanceJudgeNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static java.util.Map.entry;

@Configuration
public class WeatherGraphConfig {

    @Bean("weatherGraph")
    public CompiledGraph weatherGraph(
            WeatherRelevanceJudgeNode judgeNode,
            WeatherAnswerGenerateNode generateNode) throws GraphStateException {

        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry(WeatherGraphConstants.KEY_QUESTION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_RELEVANCE_SCORE, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_ANSWER, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_QUALITY_SCORE, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_LOOP_COUNT, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_NEXT_ACTION, new ReplaceStrategy())
        );

        StateGraph graph = new StateGraph(keyStrategy);

        graph.addNode("judge", AsyncNodeAction.node_async(judgeNode));
        graph.addNode("generate", AsyncNodeAction.node_async(generateNode));

        graph.addEdge(StateGraph.START, "judge");

        graph.addConditionalEdges("judge",
            AsyncEdgeAction.edge_async(state -> {
                double s = state.value(WeatherGraphConstants.KEY_RELEVANCE_SCORE, 0.0);
                return s >= WeatherGraphConstants.THRESHOLD_RELEVANCE ? "relevant" : "irrelevant";
            }),
            Map.of("relevant", "generate", "irrelevant", StateGraph.END)
        );



        graph.addConditionalEdges("generate",
            AsyncEdgeAction.edge_async(state -> state.value(WeatherGraphConstants.KEY_NEXT_ACTION, "break")),
            Map.of("loop", "generate", "break", StateGraph.END)
        );

        return graph.compile();
    }
}