package com.wf.strategy.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.UserInfoEntity;
import com.wf.object.query.LoginQuery;
import com.wf.service.CaptchaService;
import com.wf.strategy.LoginStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("phoneLogin")
public class PhoneLoginStrategy implements LoginStrategy {

    private static final String EMAIL_CAPTCHA_PREFIX = "captcha:email:";

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public String login(LoginQuery query) {
        String phone = query.getPhone();
        String verifyCode = query.getVerifyCode();
        String captchaKey = query.getCaptchaKey();
        String captchaCode = query.getCaptchaCode();

        if (phone == null || phone.isEmpty()) {
            throw new RuntimeException("手机号不能为空");
        }

        if (verifyCode == null || verifyCode.isEmpty()) {
            throw new RuntimeException("验证码不能为空");
        }

        if (captchaKey != null && !captchaKey.isEmpty()) {
            if (captchaCode == null || captchaCode.isEmpty()) {
                throw new RuntimeException("图形验证码不能为空");
            }
            boolean captchaValid = captchaService.verifyImageCaptcha(captchaKey, captchaCode);
            if (!captchaValid) {
                throw new RuntimeException("图形验证码错误");
            }
        }

        String redisKey = EMAIL_CAPTCHA_PREFIX + phone;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            throw new RuntimeException("验证码已过期或不存在");
        }

        if (!storedCode.equals(verifyCode)) {
            throw new RuntimeException("验证码错误");
        }

        LambdaQueryWrapper<UserInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfoEntity::getPhone, phone);
        UserInfoEntity user = userInfoMapper.selectOne(wrapper);

        if (user == null) {
            user = new UserInfoEntity();
            user.setPhone(phone);
            user.setAccountSource(0);
            user.setRole("USER");
            user.setAvailable(1);
            userInfoMapper.insert(user);
            log.info("新用户手机号注册，phone: {}", phone);
        }

        redisTemplate.delete(redisKey);
        StpUtil.login(user.getId());
        log.info("用户手机号登录成功，userId: {}", user.getId());
        return StpUtil.getTokenValue();
    }
}
