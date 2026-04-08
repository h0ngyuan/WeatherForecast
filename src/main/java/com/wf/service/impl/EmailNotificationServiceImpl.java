package com.wf.service.impl;

import com.wf.agent.entity.DisasterInfo;
import com.wf.object.vo.ReminderTaskVO;
import com.wf.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 邮件通知服务实现类
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void send(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("[紧急响应] 邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("[紧急响应] 邮件发送失败: to={}, subject={}", to, subject, e);
        }
    }

    @Override
    public void sendDisasterAlert(String to, String location, List<DisasterInfo> disasters) {
        String subject = "【灾害预警】" + location + "地区紧急通知";

        StringBuilder content = new StringBuilder();
        content.append("尊敬的用户：\n\n");
        content.append("您所在的 ").append(location).append(" 地区即将发生以下灾害，请做好防范措施：\n\n");

        for (DisasterInfo disaster : disasters) {
            content.append("【").append(disaster.getType()).append("】\n");
            content.append("时间：").append(disaster.getStartHour()).append("时至")
                    .append(disaster.getEndHour()).append("时\n");
            content.append("描述：").append(disaster.getDescription()).append("\n\n");
        }

        content.append("请密切关注天气变化，注意安全！\n\n");
        content.append("WeatherForecast 团队");

        send(to, subject, content.toString());
    }

    @Override
    public void sendReminder(String to, ReminderTaskVO task, DisasterInfo disaster) {
        String subject = "【天气提醒】" + task.getConcernWord() + "条件已满足";

        StringBuilder content = new StringBuilder();
        content.append("尊敬的用户：\n\n");
        content.append("您关注的天气条件已经满足：\n\n");
        content.append("问题：").append(task.getOriginalQuestion()).append("\n");
        content.append("地点：").append(task.getLocation()).append("\n");
        content.append("当前天气：").append(disaster.getDescription()).append("\n\n");
        content.append("祝您生活愉快！\n\n");
        content.append("WeatherForecast 团队");

        send(to, subject, content.toString());
    }

    @Override
    public void sendDisasterAlertWithText(String to, String location, String alertText) {
        String subject = "【紧急预警】" + location + "地区灾害预警通知";
        send(to, subject, alertText);
    }

    @Override
    public void sendReminderWithText(String to, String location, String reminderText) {
        String subject = "【天气提醒】" + location + "天气提醒";
        send(to, subject, reminderText);
    }
}
