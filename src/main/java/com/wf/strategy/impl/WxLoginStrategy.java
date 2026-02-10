package com.wf.strategy.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.UserInfoEntity;
import com.wf.object.query.LoginQuery;
import com.wf.strategy.LoginStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component("wxLogin")
public class WxLoginStrategy implements LoginStrategy {

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public String login(LoginQuery query) {
        try {
            String code = query.getOpenId();
            WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(code);
            String openid = session.getOpenid();
            
            LambdaQueryWrapper<UserInfoEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserInfoEntity::getWechatOpenid, openid);
            UserInfoEntity user = userInfoMapper.selectOne(wrapper);
            
            if (user == null) {
                user = new UserInfoEntity();
                user.setWechatOpenid(openid);
                user.setAccountSource(1);
                user.setRole("USER");
                user.setAvailable(1);
                userInfoMapper.insert(user);
                log.info("新用户微信注册，openid: {}", openid);
            }
            
            StpUtil.login(user.getId());
            log.info("用户微信登录成功，userId: {}", user.getId());
            return StpUtil.getTokenValue();
        } catch (Exception e) {
            log.error("微信登录失败", e);
            throw new RuntimeException("微信登录失败", e);
        }
    }
}
