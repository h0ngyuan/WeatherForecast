package com.wf.agent.graph.edge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.wf.agent.constants.WeatherGraphConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WeatherGraphEdgeActions {

    public static String judgeEdge(OverAllState state) {
        double score = state.value(WeatherGraphConstants.KEY_RELEVANCE_SCORE, 0.0);
        String result = score >= WeatherGraphConstants.THRESHOLD_RELEVANCE ? "relevant" : "irrelevant";
        
        log.info("---------- [judge边] 决策 ----------");
        log.info("相关性评分: {}, 阈值: {}", score, WeatherGraphConstants.THRESHOLD_RELEVANCE);
        log.info("决策结果: {}", result);
        log.info("---------- [judge边] 决策完成 ----------");
        
        return result;
    }

    public static String generateEdge(OverAllState state) {
        String nextAction = state.value(WeatherGraphConstants.KEY_NEXT_ACTION, "break");
        
        log.info("---------- [generate边] 决策 ----------");
        log.info("下一步操作: {}", nextAction);
        log.info("---------- [generate边] 决策完成 ----------");
        
        return nextAction;
    }
}
