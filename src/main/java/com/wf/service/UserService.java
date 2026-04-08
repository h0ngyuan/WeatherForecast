package com.wf.service;

import com.wf.object.entity.UserInfoEntity;

import java.util.List;

/**
 * 用户服务
 *
 * @author author
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfoEntity getById(Long userId);

    /**
     * 根据地区查询用户列表
     *
     * @param location 地区名称
     * @return 用户列表
     */
    List<UserInfoEntity> getUsersByLocation(String location);
}
