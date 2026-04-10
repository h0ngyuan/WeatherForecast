package com.wf.service;

import com.wf.object.request.ReminderTaskCreateRequest;
import com.wf.object.request.WeatherSubscribeRequest;
import com.wf.object.vo.ReminderTaskVO;

import java.util.List;

/**
 * 提醒任务服务接口
 *
 * 职责：
 * 1. 创建、查询、更新提醒任务
 * 2. 管理任务生命周期
 * 3. 提供任务状态变更接口
 *
 * @author author
 * @since 1.0.0
 */
public interface ReminderTaskService {

    /**
     * 创建提醒任务
     *
     * 根据用户请求创建新的天气提醒任务，任务默认状态为"未执行"。
     * 如果用户指定了预计执行时间，则使用该时间；否则使用当前时间。
     *
     * @param request 创建任务请求，包含用户ID、问题、关心条件等信息
     * @return 创建的任务ID
     * @throws IllegalArgumentException 当请求参数无效时抛出
     * @throws ServiceException 当数据库操作失败时抛出
     */
    Long createTask(ReminderTaskCreateRequest request);

    /**
     * 根据ID查询任务
     *
     * @param taskId 任务ID
     * @return 任务详情，不存在返回 null
     */
    ReminderTaskVO getTaskById(Long taskId);

    /**
     * 查询用户的任务列表
     *
     * @param userId 用户ID
     * @return 任务列表，无任务返回空列表
     */
    List<ReminderTaskVO> getTasksByUserId(Long userId);

    /**
     * 查询指定地区的待执行任务
     *
     * 查询条件：
     * - LOCATION = 指定地区
     * - TASK_STATUS = 0（未执行）或 TASK_TYPE = 1（总是提醒）
     * - AVAILABLE = 1
     * - EXPECTED_EXEC_TIME <= 当前时间
     *
     * @param location 地区名称
     * @return 待执行任务列表
     */
    List<ReminderTaskVO> getPendingTasksByLocation(String location);

    /**
     * 更新任务状态为已执行
     *
     * 仅对一次性任务（TASK_TYPE=0）有意义
     *
     * @param taskId 任务ID
     */
    void markTaskAsExecuted(Long taskId);

    /**
     * 取消任务
     *
     * 将任务标记为不可用（AVAILABLE=0）
     *
     * @param taskId 任务ID
     */
    void cancelTask(Long taskId);

    /**
     * 创建天气订阅任务
     *
     * 用户主动订阅特定天气条件，当条件满足时发送通知
     *
     * @param userId 用户ID
     * @param request 订阅请求，包含地点、天气条件等信息
     * @return 创建的任务ID
     */
    Long createSubscribeTask(Long userId, WeatherSubscribeRequest request);
}
