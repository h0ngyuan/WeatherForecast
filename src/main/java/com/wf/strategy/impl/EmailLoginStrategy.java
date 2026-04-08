package com.wf.strategy.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.UserInfoEntity;
import com.wf.object.query.LoginQuery;
import com.wf.service.CaptchaService;
import com.wf.strategy.LoginStrategy;
import com.wf.utils.LocationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Slf4j
@Component("emailLogin")
public class EmailLoginStrategy implements LoginStrategy {

    private static final String EMAIL_CAPTCHA_PREFIX = "captcha:email:";

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public String login(LoginQuery query) {
        String email = query.getEmail();
        String verifyCode = query.getVerifyCode();
        String captchaKey = query.getCaptchaKey();
        String captchaCode = query.getCaptchaCode();

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("邮箱不能为空");
        }

        if (verifyCode == null || verifyCode.isEmpty()) {
            throw new RuntimeException("邮箱验证码不能为空");
        }

//        if (captchaKey != null && !captchaKey.isEmpty()) {
//            if (captchaCode == null || captchaCode.isEmpty()) {
//                throw new RuntimeException("图形验证码不能为空");
//            }
//            boolean captchaValid = captchaService.verifyImageCaptcha(captchaKey, captchaCode);
//            if (!captchaValid) {
//                throw new RuntimeException("图形验证码错误");
//            }
//        }

        String redisKey = EMAIL_CAPTCHA_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            throw new RuntimeException("邮箱验证码已过期或不存在");
        }

        if (!storedCode.equals(verifyCode)) {
            throw new RuntimeException("邮箱验证码错误");
        }

        UserInfoEntity user = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfoEntity>()
                        .eq(UserInfoEntity::getEmail,email)
                        .eq(UserInfoEntity::getAvailable,1));

        if (user == null) {
            user = new UserInfoEntity();
            user.setNickname("Nick_"+email);
            user.setEmail(email);
            user.setAccountSource(0);
            user.setRole("USER");
            user.setAvailable(1);
            user.setWechatNotifyPermission(1);
            user.setEmailNotifyPermission(1);
            user.setPhoneNotifyPermission(1);

            // 获取用户IP对应的城市
            String city = getCityFromRequest();
            if(city!=null){
                user.setRegisterLocation(city);
                log.info("新用户邮箱注册，email: {}, 城市: {}", email, city);
            }

            userInfoMapper.insert(user);
        }

        redisTemplate.delete(redisKey);
        StpUtil.login(user.getId());
        log.info("用户邮箱登录成功，userId: {}", user.getId());
        return StpUtil.getTokenValue();
    }

    /**
     * 从请求中获取用户IP对应的城市
     */
    private String getCityFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("[EmailLoginStrategy] 无法获取请求属性");
                return null;
            }

            HttpServletRequest request = attributes.getRequest();
            String ip = getClientIp(request);

            // 使用 LocationUtils 获取城市
            Map<String, Object> locationMap = LocationUtils.getCurrentLocationMap();
            if (locationMap != null && locationMap.get("city") != null) {
                return (String) locationMap.get("city");
            }
        } catch (Exception e) {
            log.error("[EmailLoginStrategy] 获取用户城市失败", e);
        }
        return null;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
