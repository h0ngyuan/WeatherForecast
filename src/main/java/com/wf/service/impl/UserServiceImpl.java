package com.wf.service.impl;

import com.wf.object.entity.UserInfoEntity;
import com.wf.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 用户服务实现
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Override
    public UserInfoEntity getById(Long userId) {
        log.debug("[UserService] 查询用户: {}", userId);
        // TODO: 从数据库查询用户
        return null;
    }

    @Override
    public List<UserInfoEntity> getUsersByLocation(String location) {
        log.debug("[UserService] 查询地区用户: {}", location);
        // TODO: 从数据库查询地区用户列表
        return Collections.emptyList();
    }
}
