package com.wf.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.factory.LoginStrategyFactory;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.UserInfoEntity;
import com.wf.object.query.LoginQuery;
import com.wf.service.WxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WxServiceImpl implements WxService {

    @Autowired
    private LoginStrategyFactory loginStrategyFactory;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public String login(LoginQuery query) {
        String type = query.getType();
        if (type == null || type.isEmpty()) {
            type = "wx";
        }
        return loginStrategyFactory.getStrategy(type).login(query);
    }

    @Override
    public void logout() {
        StpUtil.logout();
        log.info("用户登出成功");
    }

    @Override
    public UserInfoEntity getUserInfo(Long userId) {
        LambdaQueryWrapper<UserInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfoEntity::getId, userId);
        UserInfoEntity user = userInfoMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }
}
