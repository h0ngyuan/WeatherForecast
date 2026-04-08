package com.wf.service;

import com.wf.agent.entity.DisasterInfo;
import com.wf.object.vo.ReminderTaskVO;

import java.util.List;

/**
 * 邮件通知服务接口
 *
 * @author author
 * @since 1.0.0
 */
public interface EmailNotificationService {

    /**
     * 发送普通邮件
     *
     * @param to      收件人邮箱
     * @param subject 主题
     * @param content 内容
     */
    void send(String to, String subject, String content);

    /**
     * 发送灾害预警邮件
     *
     * @param to       收件人邮箱
     * @param location 地区
     * @param disasters 灾害列表
     */
    void sendDisasterAlert(String to, String location, List<DisasterInfo> disasters);

    /**
     * 发送提醒邮件
     *
     * @param to       收件人邮箱
     * @param task     任务信息
     * @param disaster 灾害信息
     */
    void sendReminder(String to, ReminderTaskVO task, DisasterInfo disaster);

    /**
     * 发送带预警文本的灾害预警邮件
     *
     * @param to        收件人邮箱
     * @param location  地区
     * @param alertText 预警文本内容
     */
    void sendDisasterAlertWithText(String to, String location, String alertText);

    /**
     * 发送带提醒文本的提醒邮件
     *
     * @param to           收件人邮箱
     * @param location     地区
     * @param reminderText 提醒文本内容
     */
    void sendReminderWithText(String to, String location, String reminderText);
}
