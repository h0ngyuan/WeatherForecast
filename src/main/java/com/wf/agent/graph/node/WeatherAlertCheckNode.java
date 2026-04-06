package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.base.AIClient;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.UserInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WeatherAlertCheckNode implements NodeAction {

    private final AIClient aiClient;
    private final UserInfoMapper userInfoMapper;

    public WeatherAlertCheckNode(AIClient aiClient, UserInfoMapper userInfoMapper) {
        this.aiClient = aiClient;
        this.userInfoMapper = userInfoMapper;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("---------- [alertCheck节点] 开始执行 ----------");

        String originalQuestion = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        String forecastResult = state.value(WeatherGraphConstants.KEY_FORECAST_RESULT, "");
        String activityType = state.value(WeatherGraphConstants.KEY_ACTIVITY_TYPE, "");
        String concernCondition = state.value(WeatherGraphConstants.KEY_CONCERN_CONDITION, "");

        log.info("原始问题: {}", originalQuestion);
        log.info("预测结果: {}", forecastResult);
        log.info("活动类型: {}", activityType);
        log.info("关心条件: {}", concernCondition);

        String alertCheckResult = "";
        boolean needIntervention = false;
        boolean hasPermission = false;
        
        try {
            log.info("开始调用AI进行天气预警检查...");
            alertCheckResult = aiClient.performAlertCheck(originalQuestion, forecastResult, activityType, concernCondition);
            log.info("AI返回预警检查结果: {}", alertCheckResult);

            JSONObject alertJson = JSON.parseObject(alertCheckResult);
            Boolean hasAlert = alertJson.getBoolean("hasAlert");
            if (hasAlert != null && hasAlert) {
                needIntervention = true;
                log.info("检测到需要记录提醒任务");
                
                hasPermission = checkUserPermission(state);
                log.info("用户权限检查结果: {}", hasPermission);
            }
        } catch (Exception e) {
            log.error("alertCheck节点执行异常: {}", e.getMessage(), e);
        }

        log.info("是否需要记录任务: {}, 是否有权限: {}", needIntervention, hasPermission);
        log.info("---------- [alertCheck节点] 执行完成 ----------");

        Map<String, Object> result = new HashMap<>();
        result.put(WeatherGraphConstants.KEY_ALERT_CHECK_RESULT, alertCheckResult);
        result.put(WeatherGraphConstants.KEY_NEED_INTERVENTION, needIntervention);
        result.put(WeatherGraphConstants.KEY_HAS_PERMISSION, hasPermission);
        return result;
    }

    private boolean checkUserPermission(OverAllState state) {
        Long userId = state.value(WeatherGraphConstants.KEY_USER_ID, 0L);
        if (userId == 0L) {
            log.warn("未获取到用户ID，无法检查权限");
            return false;
        }

        UserInfoEntity userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }

        // 检查用户是否有任一通知权限且绑定了对应联系方式
//        boolean hasPhonePermission = userInfo.getPhoneNotifyPermission() != null
//                && userInfo.getPhoneNotifyPermission() == 1
//                && userInfo.getPhone() != null
//                && !userInfo.getPhone().isEmpty();

        boolean hasEmailPermission = userInfo.getEmailNotifyPermission() != null
                && userInfo.getEmailNotifyPermission() == 1
                && userInfo.getEmail() != null
                && !userInfo.getEmail().isEmpty();
//
//        boolean hasWechatPermission = userInfo.getWechatNotifyPermission() != null
//                && userInfo.getWechatNotifyPermission() == 1
//                && userInfo.getWechatOpenid() != null
//                && !userInfo.getWechatOpenid().isEmpty();

//        boolean hasPermission = hasPhonePermission || hasEmailPermission || hasWechatPermission;

//        log.info("用户权限检查: userId={}, phonePermission={}, emailPermission={}, wechatPermission={}",
//                userId, hasPhonePermission, hasEmailPermission, hasWechatPermission);

        return hasEmailPermission;
    }
}
