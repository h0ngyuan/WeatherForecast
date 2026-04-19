package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.object.request.ReminderTaskCreateRequest;
import com.wf.service.ReminderTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 天气预测流程 - 提醒任务写入节点
 *
 * 职责：
 * 将用户确认的提醒任务写入数据库
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
public class WeatherWriteTaskNode implements NodeAction {

    private final ReminderTaskService reminderTaskService;

    public WeatherWriteTaskNode(ReminderTaskService reminderTaskService) {
        this.reminderTaskService = reminderTaskService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("========== [writeTask节点] 开始执行 ==========");

        Long userId = state.value(WeatherGraphConstants.KEY_USER_ID, 0L);
        String originalQuestion = state.value(WeatherGraphConstants.KEY_QUESTION, "");
        String alertCheckResult = state.value(WeatherGraphConstants.KEY_ALERT_CHECK_RESULT, "");
        String activityType = state.value(WeatherGraphConstants.KEY_ACTIVITY_TYPE, "");
        String concernCondition = state.value(WeatherGraphConstants.KEY_CONCERN_CONDITION, "");

        log.info("[writeTask] 用户ID: {}", userId);
        log.info("[writeTask] 原始问题: {}", originalQuestion);
        log.info("[writeTask] 预警检查结果: {}", alertCheckResult);
        log.info("[writeTask] 活动类型: {}", activityType);
        log.info("[writeTask] 关心条件: {}", concernCondition);

        if (userId == 0L) {
            log.error("[writeTask] 用户ID为空，无法创建提醒任务");
            return Map.of();
        }

        try {
            log.info("[writeTask] 开始清理 Markdown 代码块...");
            String cleanedJson = cleanMarkdownCodeBlock(alertCheckResult);
            log.info("[writeTask] 清理后的 JSON: {}", cleanedJson);

            log.info("[writeTask] 开始解析 JSON...");
            JSONObject alertJson = JSON.parseObject(cleanedJson);
            log.info("[writeTask] JSON 解析成功");

            JSONObject reminderTask = alertJson.getJSONObject("reminderTask");

            if (reminderTask == null) {
                log.warn("[writeTask] 预警检查结果中没有 reminderTask，跳过写入");
                return Map.of();
            }
            log.info("[writeTask] 找到 reminderTask: {}", reminderTask);

            // 解析任务信息
            String taskTypeStr = reminderTask.getString("taskType");
            String monitoringPeriod = reminderTask.getString("monitoringPeriod");
            String notifyCondition = reminderTask.getString("notifyCondition");
            String location = reminderTask.getString("location");

            log.info("[writeTask] taskType={}, monitoringPeriod={}, notifyCondition={}, location={}",
                    taskTypeStr, monitoringPeriod, notifyCondition, location);

            // 如果没有location，尝试从requestInfo解析
            if (location == null || location.isEmpty()) {
                log.info("[writeTask] location 为空，尝试从 alertJson 中提取");
                location = extractLocationFromAlertCheck(alertJson);
                log.info("[writeTask] 提取到的 location: {}", location);
            }

            // 解析任务类型 (0=一次，1=总是)
            Integer taskType = parseTaskType(taskTypeStr);

            // 解析关心条件（天气码值）
            Integer concernConditionCode = parseConcernCondition(concernCondition);

            // 解析监控时间范围
            TimeRange timeRange = parseMonitoringPeriod(monitoringPeriod);

            // 解析 AI 评估的 disasterLevel
            Integer disasterLevel = reminderTask.getInteger("disasterLevel");

            // 如果 AI 没有返回，使用默认值
            if (disasterLevel == null) {
                disasterLevel = 3; // 默认3级（轻微）
            }

            // 构建创建请求
            ReminderTaskCreateRequest request = new ReminderTaskCreateRequest();
            request.setUserId(userId);
            request.setOriginalQuestion(originalQuestion);
            request.setConcernWord(activityType);
            request.setConcernCondition(concernConditionCode);
            request.setTaskType(taskType);
            request.setNotifyCondition(notifyCondition);
            request.setLocation(location);
            request.setExpectedEarliestTime(timeRange.startTime);
            request.setExpectedLatestTime(timeRange.endTime);
            request.setDisasterLevel(disasterLevel);

            log.info("[writeTask] 准备写入提醒任务: userId={}, location={}, taskType={}, concernCondition={}, disasterLevel={}",
                    userId, location, taskType, concernConditionCode, disasterLevel);
            log.info("[writeTask] 时间范围: {} 至 {}", timeRange.startTime, timeRange.endTime);

            Long taskId = reminderTaskService.createTask(request);
            log.info("[writeTask] 提醒任务写入成功! taskId={}", taskId);

        } catch (Exception e) {
            log.error("[writeTask] 写入提醒任务失败", e);
        }

        log.info("========== [writeTask节点] 执行完成 ==========");

        return Map.of();
    }

    /**
     * 清理 Markdown 代码块包裹
     */
    private String cleanMarkdownCodeBlock(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String cleaned = input.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline >= 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            } else {
                cleaned = cleaned.substring(3);
            }
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    /**
     * 从预警检查结果中提取位置信息
     */
    private String extractLocationFromAlertCheck(JSONObject alertJson) {
        try {
            // 尝试从requestInfo中解析
            String requestInfo = alertJson.getString("requestInfo");
            if (requestInfo != null && !requestInfo.isEmpty()) {
                JSONObject requestInfoJson = JSON.parseObject(requestInfo);
                if (requestInfoJson.containsKey("city")) {
                    return requestInfoJson.getString("city");
                }
                if (requestInfoJson.containsKey("location")) {
                    return requestInfoJson.getString("location");
                }
            }
        } catch (Exception e) {
            log.warn("从alertCheck解析location失败", e);
        }
        return "未知地点";
    }

    /**
     * 解析任务类型
     */
    private Integer parseTaskType(String taskTypeStr) {
        if (taskTypeStr == null || taskTypeStr.isEmpty()) {
            return 0; // 默认一次
        }
        // 如果包含"总是"、"长期"等关键词，返回1
        if (taskTypeStr.contains("总是") || taskTypeStr.contains("长期") || taskTypeStr.contains("持续")) {
            return 1;
        }
        return 0; // 默认一次
    }

    /**
     * 解析关心条件为天气码值
     */
    private Integer parseConcernCondition(String concernCondition) {
        if (concernCondition == null || concernCondition.isEmpty()) {
            return null;
        }
        try {
            // 尝试直接解析为整数
            return Integer.parseInt(concernCondition.trim());
        } catch (NumberFormatException e) {
            log.warn("无法解析关心条件为天气码值: {}", concernCondition);
            return null;
        }
    }

    /**
     * 解析监控时间段
     */
    private TimeRange parseMonitoringPeriod(String monitoringPeriod) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now;
        LocalDateTime endTime = now.plusDays(1); // 默认监控1天

        if (monitoringPeriod == null || monitoringPeriod.isEmpty()) {
            return new TimeRange(startTime, endTime);
        }

        try {
            // 尝试解析 "2024-01-01 08:00 至 2024-01-01 20:00" 格式
            if (monitoringPeriod.contains("至")) {
                String[] parts = monitoringPeriod.split("至");
                if (parts.length == 2) {
                    startTime = parseDateTime(parts[0].trim());
                    endTime = parseDateTime(parts[1].trim());
                }
            }
        } catch (Exception e) {
            log.warn("解析监控时间段失败: {}", monitoringPeriod, e);
        }

        return new TimeRange(startTime, endTime);
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // 尝试多种格式
            java.time.format.DateTimeFormatter[] formatters = {
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            };

            for (java.time.format.DateTimeFormatter formatter : formatters) {
                try {
                    if (dateTimeStr.length() <= 10) {
                        // 只有日期，加上时间
                        return LocalDateTime.parse(dateTimeStr + " 00:00:00",
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                    return LocalDateTime.parse(dateTimeStr, formatter);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.warn("无法解析日期时间: {}", dateTimeStr);
        }
        return LocalDateTime.now();
    }

    /**
     * 时间范围记录
     */
    private record TimeRange(LocalDateTime startTime, LocalDateTime endTime) {}
}
