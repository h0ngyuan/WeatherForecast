package com.wf.agent.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.agent.graph.node.WeatherAnswerGenerateNode;
import com.wf.agent.graph.node.WeatherAlertCheckNode;
import com.wf.agent.graph.node.WeatherForecastNode;
import com.wf.agent.graph.node.WeatherHumanFeedbackNode;
import com.wf.agent.graph.node.WeatherRelevanceJudgeNode;
import com.wf.agent.graph.node.WeatherSemanticTransformNode;
import com.wf.agent.graph.node.WeatherWriteTaskNode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static java.util.Map.entry;

@Slf4j
@Configuration
public class WeatherGraphConfig {

    @Autowired
    private RedissonClient redissonClient;

    @Bean("weatherGraph")
    public CompiledGraph weatherGraph(
            WeatherRelevanceJudgeNode judgeNode,
            WeatherSemanticTransformNode transformNode,
            WeatherForecastNode forecastNode,
            WeatherAlertCheckNode alertCheckNode,
            WeatherHumanFeedbackNode humanFeedbackNode,
            WeatherWriteTaskNode writeTaskNode,
            WeatherAnswerGenerateNode generateNode) throws GraphStateException {

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║               WeatherGraph 工作流构建开始                     ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        log.info("【步骤1】配置状态键策略 (KeyStrategy)");
        KeyStrategyFactory keyStrategy = () -> Map.ofEntries(
            entry(WeatherGraphConstants.KEY_QUESTION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_RELEVANCE_SCORE, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_TRANSFORMED_QUESTION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_WEATHER_CODE_QUERY, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_FORECAST_RESULT, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_ALERT_CHECK_RESULT, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_NEED_INTERVENTION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_HAS_PERMISSION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_HUMAN_FEEDBACK, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_GENERATE_RESULT, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_ANSWER, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_QUALITY_SCORE, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_LOOP_COUNT, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_NEXT_ACTION, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_ACTIVITY_TYPE, new ReplaceStrategy()),
            entry(WeatherGraphConstants.KEY_CONCERN_CONDITION, new ReplaceStrategy())
        );
        log.info("  └─ 已注册 {} 个状态键", 16);

        StateGraph graph = new StateGraph(keyStrategy);

        log.info("【步骤2】注册工作流节点");
        graph.addNode("judge", AsyncNodeAction.node_async(judgeNode));
        graph.addNode("transform", AsyncNodeAction.node_async(transformNode));
        graph.addNode("forecast", AsyncNodeAction.node_async(forecastNode));
        graph.addNode("alertCheck", AsyncNodeAction.node_async(alertCheckNode));
        graph.addNode("humanFeedback", AsyncNodeAction.node_async(humanFeedbackNode));
        graph.addNode("writeTask", AsyncNodeAction.node_async(writeTaskNode));
        graph.addNode("generate", AsyncNodeAction.node_async(generateNode));
        log.info("  ├─ [judge]        问题相关性判断节点");
        log.info("  ├─ [transform]    语义转化与规范化节点");
        log.info("  ├─ [forecast]     天气预测数据获取节点");
        log.info("  ├─ [alertCheck]   预警检查与权限判断节点");
        log.info("  ├─ [humanFeedback] 人工确认权限节点");
        log.info("  ├─ [writeTask]    提醒任务写入节点");
        log.info("  └─ [generate]     答案生成节点");

        log.info("【步骤3】配置工作流边连接");
        graph.addEdge(StateGraph.START, "judge");
        log.info("  └─ START -> judge");

        graph.addConditionalEdges("judge",
            AsyncEdgeAction.edge_async(state -> state.value(WeatherGraphConstants.KEY_RELEVANCE_SCORE, 0.0) >= WeatherGraphConstants.THRESHOLD_RELEVANCE ? "relevant" : "irrelevant"),
            Map.of("relevant", "transform", "irrelevant", StateGraph.END)
        );
        log.info("  └─ judge --(相关性>=阈值?)--> transform / END");

        graph.addEdge("transform", "forecast");
        log.info("添加边: transform -> forecast");

        graph.addEdge("forecast", "alertCheck");
        log.info("  └─ forecast -> alertCheck");

        graph.addConditionalEdges("alertCheck",
            AsyncEdgeAction.edge_async(state -> 
                state.value(WeatherGraphConstants.KEY_NEED_INTERVENTION, false) 
                    ? (state.value(WeatherGraphConstants.KEY_HAS_PERMISSION, false) ? "has_permission" : "no_permission") 
                    : "no_task"
            ),
            Map.of("no_task", "generate", "has_permission", "writeTask", "no_permission", "humanFeedback")
        );
        log.info("  └─ alertCheck --(需要记录任务?)-->");
        log.info("       ├─ 无任务 -> generate");
        log.info("       ├─ 有权限 -> writeTask");
        log.info("       └─ 无权限 -> humanFeedback");

        graph.addConditionalEdges("humanFeedback",
            AsyncEdgeAction.edge_async(state -> state.value(WeatherGraphConstants.KEY_HUMAN_FEEDBACK, false) ? "approved" : "rejected"),
            Map.of("approved", "writeTask", "rejected", StateGraph.END)
        );
        log.info("  └─ humanFeedback --(用户同意?)--> writeTask / END");

        graph.addEdge("writeTask", "generate");
        log.info("  └─ writeTask -> generate");

        graph.addConditionalEdges("generate",
            AsyncEdgeAction.edge_async(state -> state.value(WeatherGraphConstants.KEY_NEXT_ACTION, "break")),
            Map.of("loop", "generate", "break", StateGraph.END)
        );
        log.info("  └─ generate --(质量达标?)--> generate(自循环) / END");

        log.info("【步骤4】配置编译选项");
        CompileConfig compileConfig = CompileConfig.builder()
            .saverConfig(SaverConfig.builder()
                .register(new RedisSaver.Builder().redisson(redissonClient).build())
                .build())
            .interruptBefore("humanFeedback")
            .build();
        log.info("  ├─ 状态持久化: RedisSaver");
        log.info("  └─ 中断节点: humanFeedback");

        CompiledGraph compiledGraph = graph.compile(compileConfig);

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║               WeatherGraph 工作流构建完成                     ║");
        log.info("║                                                              ║");
        log.info("║  流程图:                                                     ║");
        log.info("║  START -> judge -> transform -> forecast -> alertCheck       ║");
        log.info("║                                              │                ║");
        log.info("║                     ┌────────────────────────┼─────────────┐  ║");
        log.info("║                     ↓                        ↓             ↓  ║");
        log.info("║                  generate              writeTask    humanFeedback ║");
        log.info("║                  (自循环)                    ↓             ↓  ║");
        log.info("║                                          generate      writeTask ║");
        log.info("║                                              │                ║");
        log.info("║                                              └──────> END     ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        return compiledGraph;
    }
}