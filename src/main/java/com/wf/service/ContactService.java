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
}
