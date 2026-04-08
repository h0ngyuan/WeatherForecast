package com.wf.agent.entity;

import lombok.Data;

/**
 * 灾害信息
 *
 * @author author
 * @since 1.0.0
 */
@Data
public class DisasterInfo {

    /** 灾害类型 */
    private String type;

    /** 天气码值 */
    private Integer weatherCode;

    /** 开始时间（第几小时） */
    private Integer startHour;

    /** 结束时间（第几小时） */
    private Integer endHour;

    /** 灾害描述 */
    private String description;

    /** 灾害级别（1=严重，2=中等，3=轻微） */
    private Integer level;
}
