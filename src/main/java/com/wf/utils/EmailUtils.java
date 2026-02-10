package com.wf.utils;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Slf4j
public class EmailUtils {

    private static String fromEmail;
    private static JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    public void setFromEmail(String fromEmail) {
        EmailUtils.fromEmail = fromEmail;
    }

    @Value("${spring.mail.host}")
    public void setMailSender(JavaMailSender mailSender) {
        EmailUtils.mailSender = mailSender;
    }

    public static void sendCaptcha(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("验证码");

            String content = buildCaptchaContent(code);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("邮箱验证码发送成功，toEmail: {}, code: {}", toEmail, code);
        } catch (Exception e) {
            log.error("邮箱验证码发送异常，toEmail: {}", toEmail, e);
            throw new RuntimeException("邮箱验证码发送异常", e);
        }
    }

    public static void sendHtmlEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("HTML邮件发送成功，toEmail: {}", toEmail);
        } catch (Exception e) {
            log.error("HTML邮件发送异常，toEmail: {}", toEmail, e);
            throw new RuntimeException("HTML邮件发送异常", e);
        }
    }

    public static void sendSimpleEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false);

            mailSender.send(message);
            log.info("简单邮件发送成功，toEmail: {}", toEmail);
        } catch (Exception e) {
            log.error("简单邮件发送异常，toEmail: {}", toEmail, e);
            throw new RuntimeException("简单邮件发送异常", e);
        }
    }

    private static String buildCaptchaContent(String code) {
        return "<html><body>" +
                "<h2>您的验证码是：<span style='color: #ff6600; font-size: 24px;'>" + code + "</span></h2>" +
                "<p>验证码有效期为5分钟，请尽快使用。</p>" +
                "<p>如果这不是您的操作，请忽略此邮件。</p>" +
                "</body></html>";
    }
}
