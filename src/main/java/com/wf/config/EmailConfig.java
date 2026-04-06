package com.wf.config;

import com.wf.utils.EmailUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 邮件配置类
 * 负责初始化邮件工具类
 */
@Slf4j
@Configuration
public class EmailConfig {

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private JavaMailSender mailSender;

    @PostConstruct
    public void init() {
        EmailUtils.init(fromEmail, mailSender);
        log.info("邮件工具初始化完成，发件人: {}", fromEmail);
    }
}
