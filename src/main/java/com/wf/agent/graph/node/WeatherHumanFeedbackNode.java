package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.agent.constants.WeatherGraphConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WeatherHumanFeedbackNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("---------- [humanFeedback节点] 开始执行 ----------");

        Boolean humanFeedback = state.value(WeatherGraphConstants.KEY_HUMAN_FEEDBACK, false);
        log.info("用户反馈: {}", humanFeedback);

        log.info("---------- [humanFeedback节点] 执行完成 ----------");

        return Map.of();
    }
}
