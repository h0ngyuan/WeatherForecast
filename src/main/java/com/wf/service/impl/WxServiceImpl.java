package com.wf.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.dev33.satoken.stp.StpUtil;
import com.wf.object.query.LoginQuery;
import com.wf.service.WxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WxServiceImpl implements WxService {

    @Autowired
    private WxMaService wxMaService;

    @Override
    public String login(LoginQuery query) {
        try {
//            这边作为模拟，暂不调用微信开发者工具获取测试code
//            WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(code);
//            String openid = session.getOpenid();
//            StpUtil.login(openid);
            String openId = query.getQtCode();
            String mockOpenId = openId +"aacc";
            StpUtil.login(mockOpenId);
            return StpUtil.getTokenValue();
        } catch (Exception e) {
            throw new RuntimeException("微信登录失败", e);
        }

    }
}
