package com.wf.object.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒任务实体类
 *
 * 对应数据库表 WEATHER_REMINDER_TASK
 *
 * @author author
 * @since 1.0.0
 */
@Data
public class ReminderTaskEntity {

    /** 任务ID */
    private Long id;

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

    /** 任务状态：0=未执行，1=已执行 */
    private Integer taskStatus;

    /** 预计最早执行时间 */
    private LocalDateTime expectedEarliestTime;

    /** 邮件通知 */
    private Integer notifyByEmail;

    /** 短信通知 */
    private Integer notifyBySms;

    /** 微信通知 */
    private Integer notifyByWechat;

    /** 是否可用 */
    private Integer available;

    /** 灾害等级：1=一级，2=二级，3=三级 */
    private Integer disasterLevel;

    /** 预计最晚执行时间 */
    private LocalDateTime expectedLatestTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
