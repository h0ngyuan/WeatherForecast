package com.wf.service.impl;

import com.wf.mapper.ReminderTaskMapper;
import com.wf.object.entity.ReminderTaskEntity;
import com.wf.object.request.ReminderTaskCreateRequest;
import com.wf.object.request.WeatherSubscribeRequest;
import com.wf.object.vo.ReminderTaskVO;
import com.wf.service.ReminderTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 提醒任务服务实现类
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderTaskServiceImpl implements ReminderTaskService {

    private final ReminderTaskMapper reminderTaskMapper;

    @Override
    public Long createTask(ReminderTaskCreateRequest request) {
        log.info("创建提醒任务, userId={}, question={}",
                request.getUserId(), request.getOriginalQuestion());

        // 构建实体对象
        ReminderTaskEntity entity = new ReminderTaskEntity();
        BeanUtils.copyProperties(request, entity);

        // 设置默认值
        if (entity.getTaskType() == null) {
            entity.setTaskType(0);
        }
        if (entity.getTaskStatus() == null) {
            entity.setTaskStatus(0);
        }
        if (entity.getExpectedEarliestTime() == null) {
            entity.setExpectedEarliestTime(LocalDateTime.now());
        }
        if (entity.getDisasterLevel() == null) {
            entity.setDisasterLevel(3); // 默认3级（轻微）
        }
        entity.setNotifyByEmail(1);
        entity.setNotifyBySms(0);
        entity.setNotifyByWechat(0);
        entity.setAvailable(1);

        // 插入数据库
        reminderTaskMapper.insert(entity);

        log.info("提醒任务创建成功, taskId={}", entity.getId());
        return entity.getId();
    }

    @Override
    public ReminderTaskVO getTaskById(Long taskId) {
        ReminderTaskEntity entity = reminderTaskMapper.selectById(taskId);
        if (entity == null || entity.getAvailable() == 0) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public List<ReminderTaskVO> getTasksByUserId(Long userId) {
        List<ReminderTaskEntity> entities = reminderTaskMapper.selectByUserId(userId);
        return entities.stream()
                .filter(e -> e.getAvailable() == 1)
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReminderTaskVO> getPendingTasksByLocation(String location) {
        List<ReminderTaskEntity> entities = reminderTaskMapper.selectPendingByLocation(
                location, LocalDateTime.now());
        return entities.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public void markTaskAsExecuted(Long taskId) {
        reminderTaskMapper.updateTaskStatus(taskId, 1);
        log.info("任务标记为已执行, taskId={}", taskId);
    }

    @Override
    public void cancelTask(Long taskId) {
        reminderTaskMapper.updateAvailable(taskId, 0);
        log.info("任务已取消, taskId={}", taskId);
    }

    @Override
    public Long createSubscribeTask(Long userId, WeatherSubscribeRequest request) {
        log.info("创建天气订阅任务, userId={}, subscribeName={}, location={}, weatherCodes={}",
                userId, request.getSubscribeName(), request.getLocation(), request.getWeatherCodes());

        // 构建任务列表
        List<ReminderTaskEntity> entities = new java.util.ArrayList<>();
        for (Integer weatherCode : request.getWeatherCodes()) {
            ReminderTaskEntity entity = new ReminderTaskEntity();
            entity.setUserId(userId);
            entity.setOriginalQuestion(request.getSubscribeName());
            entity.setConcernWord("天气订阅");
            entity.setConcernCondition(weatherCode);
            entity.setTaskType(request.getTaskType() != null ? request.getTaskType() : 1); // 默认总是提醒
            entity.setNotifyCondition(request.getNotifyCondition());
            entity.setLocation(request.getLocation());
            entity.setExpectedEarliestTime(request.getExpectedEarliestTime() != null ?
                    request.getExpectedEarliestTime() : LocalDateTime.now());
            // 默认永久监控，不设置最晚时间（null表示永久）
            entity.setExpectedLatestTime(request.getExpectedLatestTime());
            entity.setDisasterLevel(request.getDisasterLevel() != null ? request.getDisasterLevel() : 3);
            entity.setTaskStatus(0);
            entity.setNotifyByEmail(1);
            entity.setNotifyBySms(0);
            entity.setNotifyByWechat(0);
            entity.setAvailable(1);
            entities.add(entity);
        }

        // 批量插入
        reminderTaskMapper.batchInsert(entities);
        log.info("天气订阅任务批量创建完成, 共{}个任务", entities.size());

        // 返回第一个任务的ID
        return entities.isEmpty() ? null : entities.get(0).getId();
    }

    /**
     * 实体转换为VO
     */
    private ReminderTaskVO convertToVO(ReminderTaskEntity entity) {
        ReminderTaskVO vo = new ReminderTaskVO();
        BeanUtils.copyProperties(entity, vo);

        // 设置描述字段
        vo.setTaskTypeDesc(entity.getTaskType() == 1 ? "总是提醒" : "一次提醒");
        vo.setTaskStatusDesc(entity.getTaskStatus() == 1 ? "已执行" : "未执行");

        return vo;
    }
}
