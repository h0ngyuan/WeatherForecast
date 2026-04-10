package com.wf.object.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒任务视图对象
 *
 * 用于前端展示和层间传递
 *
 * @author author
 * @since 1.0.0
 */
@Data
public class ReminderTaskVO {

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

    /** 任务类型描述 */
    private String taskTypeDesc;

    /** 监控地点 */
    private String location;

    /** 任务状态：0=未执行，1=已执行 */
    private Integer taskStatus;

    /** 任务状态描述 */
    private String taskStatusDesc;

    /** 预计执行时间 */
    private LocalDateTime expectedExecTime;

    /** 灾害等级：1=一级，2=二级，3=三级 */
    private Integer disasterLevel;

    /** 最早时间 */
    private LocalDateTime startTime;

    /** 最晚时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
