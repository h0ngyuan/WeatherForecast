package com.wf.service;

import com.wf.object.entity.UserInfoEntity;
import com.wf.object.query.NotifySettingQuery;

public interface ContactService {

    boolean checkPhoneBound(Long userId);

    boolean checkEmailBound(Long userId);

    boolean bindPhone(Long userId, String phone, String code);

    boolean bindEmail(Long userId, String email, String code);

    void sendPhoneCaptcha(String phone);

    void sendEmailCaptcha(String email);

    UserInfoEntity getNotifySettings(Long userId);

    void updateNotifySettings(Long userId, NotifySettingQuery query);

    /**
     * 直接绑定手机号（用于人工干预场景，不验证验证码）
     */
    boolean bindPhoneDirect(Long userId, String phone);

    /**
     * 直接绑定邮箱（用于人工干预场景，不验证验证码）
     */
    boolean bindEmailDirect(Long userId, String email);
}
