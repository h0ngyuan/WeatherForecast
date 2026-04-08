package com.wf.object.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建提醒任务请求
 *
 * @author author
 * @since 1.0.0
 */
@Data
public class ReminderTaskCreateRequest {

    /** 用户ID */
    private Long userId;

    /** 用户原始问题 */
    private String originalQuestion;

    /** 关键词 */
    private String concernWord;

    /** 关心条件（天气码值） */
    private Integer concernCondition;

    /** 任务类型：0=一次，1=总是 */
    private Integer taskType;

    /** 通知条件描述 */
    private String notifyCondition;

    /** 监控地点 */
    private String location;

    /** 预计执行时间 */
    private LocalDateTime expectedExecTime;
}
