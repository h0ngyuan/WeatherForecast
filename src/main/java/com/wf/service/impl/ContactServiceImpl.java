package com.wf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.UserInfoEntity;
import com.wf.object.query.NotifySettingQuery;
import com.wf.service.ContactService;
import com.wf.utils.EmailUtils;
import com.wf.utils.SMSUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ContactServiceImpl implements ContactService {

    private static final String PHONE_CAPTCHA_PREFIX = "captcha:phone:";
    private static final String EMAIL_CAPTCHA_PREFIX = "captcha:email:";
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    private static final int CAPTCHA_LENGTH = 4;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean checkPhoneBound(Long userId) {
        return checkContactBound(userId, UserInfoEntity::getPhone);
    }

    @Override
    public boolean checkEmailBound(Long userId) {
        return checkContactBound(userId, UserInfoEntity::getEmail);
    }

    @Override
    public boolean bindPhone(Long userId, String phone, String code) {
        return bindContact(userId, phone, code, PHONE_CAPTCHA_PREFIX, UserInfoEntity::getPhone);
    }

    @Override
    public boolean bindEmail(Long userId, String email, String code) {
        return bindContact(userId, email, code, EMAIL_CAPTCHA_PREFIX, UserInfoEntity::getEmail);
    }

    @Override
    public void sendPhoneCaptcha(String phone) {
        String code = generateRandomCaptcha();
        String redisKey = PHONE_CAPTCHA_PREFIX + phone;
        redisTemplate.opsForValue().set(redisKey, code, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("手机号验证码生成成功，phone: {}, code: {}（短信服务暂时禁用，请使用邮箱登录）", phone, code);
    }

    @Override
    public void sendEmailCaptcha(String email) {
        String code = generateRandomCaptcha();
        String redisKey = EMAIL_CAPTCHA_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, code, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);
        EmailUtils.sendCaptcha(email, code);
    }

    private boolean checkContactBound(Long userId, java.util.function.Function<UserInfoEntity, String> contactGetter) {
        LambdaQueryWrapper<UserInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfoEntity::getId, userId);
        UserInfoEntity user = userInfoMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String contact = contactGetter.apply(user);
        boolean isBound = contact != null && !contact.isEmpty();
        log.info("检查用户联系方式绑定状态，userId: {}, isBound: {}", userId, isBound);
        return isBound;
    }

    private boolean bindContact(Long userId, String contact, String code, String prefix, 
                                java.util.function.Function<UserInfoEntity, String> contactGetter) {
        if (contact == null || contact.isEmpty()) {
            throw new RuntimeException("联系方式不能为空");
        }

        if (code == null || code.isEmpty()) {
            throw new RuntimeException("验证码不能为空");
        }

        String redisKey = prefix + contact;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            throw new RuntimeException("验证码已过期或不存在");
        }

        if (!storedCode.equals(code)) {
            throw new RuntimeException("验证码错误");
        }

        LambdaQueryWrapper<UserInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfoEntity::getId, userId);
        UserInfoEntity user = userInfoMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String existingContact = contactGetter.apply(user);
        if (existingContact != null && !existingContact.isEmpty()) {
            throw new RuntimeException("该联系方式已绑定");
        }

        LambdaUpdateWrapper<UserInfoEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserInfoEntity::getId, userId);
        
        if (prefix.contains("phone")) {
            updateWrapper.set(UserInfoEntity::getPhone, contact);
        } else {
            updateWrapper.set(UserInfoEntity::getEmail, contact);
        }
        
        int updated = userInfoMapper.update(null, updateWrapper);
        if (updated > 0) {
            redisTemplate.delete(redisKey);
            log.info("用户绑定联系方式成功，userId: {}, contact: {}", userId, contact);
            return true;
        }
        return false;
    }

    private String generateRandomCaptcha() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    @Override
    public UserInfoEntity getNotifySettings(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        LambdaQueryWrapper<UserInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfoEntity::getId, userId);
        UserInfoEntity user = userInfoMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserInfoEntity result = new UserInfoEntity();
        result.setPhone(user.getPhone());
        result.setEmail(user.getEmail());
        result.setWechatOpenid(user.getWechatOpenid());
        result.setPhoneNotifyPermission(user.getPhoneNotifyPermission());
        result.setEmailNotifyPermission(user.getEmailNotifyPermission());
        result.setWechatNotifyPermission(user.getWechatNotifyPermission());

        return result;
    }

    @Override
    public void updateNotifySettings(Long userId, NotifySettingQuery query) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (query == null) {
            throw new RuntimeException("参数不能为空");
        }

        LambdaQueryWrapper<UserInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfoEntity::getId, userId);
        UserInfoEntity user = userInfoMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (query.getWechatNotifyPermission() == 1 && (user.getWechatOpenid() == null || user.getWechatOpenid().isEmpty())) {
            throw new RuntimeException("未绑定微信，无法开启微信通知权限");
        }
        if (query.getEmailNotifyPermission() == 1 && (user.getEmail() == null || user.getEmail().isEmpty())) {
            throw new RuntimeException("未绑定邮箱，无法开启邮箱通知权限");
        }
        if (query.getPhoneNotifyPermission() == 1 && (user.getPhone() == null || user.getPhone().isEmpty())) {
            throw new RuntimeException("未绑定手机号，无法开启手机号通知权限");
        }

        LambdaUpdateWrapper<UserInfoEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserInfoEntity::getId, userId);
        updateWrapper.set(UserInfoEntity::getWechatNotifyPermission, query.getWechatNotifyPermission());
        updateWrapper.set(UserInfoEntity::getEmailNotifyPermission, query.getEmailNotifyPermission());
        updateWrapper.set(UserInfoEntity::getPhoneNotifyPermission, query.getPhoneNotifyPermission());

        userInfoMapper.update(null, updateWrapper);
        log.info("用户通知设置更新成功，userId: {}", userId);
    }
}
