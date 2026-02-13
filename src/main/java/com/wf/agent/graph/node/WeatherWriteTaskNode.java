package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.constants.WeatherGraphConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WeatherWriteTaskNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("---------- [writeTask节点] 开始执行 ----------");

        String originalQuestion = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        String alertCheckResult = state.value(WeatherGraphConstants.KEY_ALERT_CHECK_RESULT, "");
        String activityType = state.value(WeatherGraphConstants.KEY_ACTIVITY_TYPE, "");
        String concernCondition = state.value(WeatherGraphConstants.KEY_CONCERN_CONDITION, "");

        log.info("原始问题: {}", originalQuestion);
        log.info("预警检查结果: {}", alertCheckResult);
        log.info("活动类型: {}", activityType);
        log.info("关心条件: {}", concernCondition);

        try {
            JSONObject alertJson = JSON.parseObject(alertCheckResult);
            JSONObject reminderTask = alertJson.getJSONObject("reminderTask");
            
            if (reminderTask != null) {
                String taskType = reminderTask.getString("taskType");
                String currentPrediction = reminderTask.getString("currentPrediction");
                String monitoringPeriod = reminderTask.getString("monitoringPeriod");
                String notifyCondition = reminderTask.getString("notifyCondition");

                log.info("写入提醒任务: taskType={}, concernCondition={}, monitoringPeriod={}, notifyCondition={}",
                        taskType, concernCondition, monitoringPeriod, notifyCondition);

                // TODO: 实际写入数据库的逻辑
                // taskService.createTask(taskType, concernCondition, monitoringPeriod, notifyCondition, originalQuestion);
                
                log.info("提醒任务写入成功");
            }
        } catch (Exception e) {
            log.error("写入提醒任务失败", e);
        }

        log.info("---------- [writeTask节点] 执行完成 ----------");

        return Map.of();
    }
}
