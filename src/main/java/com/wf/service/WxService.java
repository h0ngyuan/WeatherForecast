package com.wf.service;

import com.wf.object.entity.UserInfoEntity;
import com.wf.object.query.LoginQuery;

public interface WxService {
    String login(LoginQuery query);

    void logout();

    UserInfoEntity getUserInfo(Long userId);
}
