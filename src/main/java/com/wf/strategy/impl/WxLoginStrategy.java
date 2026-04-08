package com.wf.strategy.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.UserInfoEntity;
import com.wf.object.query.LoginQuery;
import com.wf.strategy.LoginStrategy;
import com.wf.utils.LocationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

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
                user.setWechatNotifyPermission(1);
                user.setEmailNotifyPermission(1);
                user.setPhoneNotifyPermission(1);

                // 获取用户IP对应的城市
                String city = getCityFromRequest();
                user.setRegisterLocation(city);
                log.info("新用户微信注册，openid: {}, 城市: {}", openid, city);

                userInfoMapper.insert(user);
            }
            
            StpUtil.login(user.getId());
            log.info("用户微信登录成功，userId: {}", user.getId());
            return StpUtil.getTokenValue();
        } catch (Exception e) {
            log.error("微信登录失败", e);
            throw new RuntimeException("微信登录失败", e);
        }
    }

    /**
     * 从请求中获取用户IP对应的城市
     */
    private String getCityFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("[WxLoginStrategy] 无法获取请求属性");
                return "未知城市";
            }

            HttpServletRequest request = attributes.getRequest();
            String ip = getClientIp(request);

            // 使用 LocationUtils 获取城市
            Map<String, Object> locationMap = LocationUtils.getCurrentLocationMap();
            if (locationMap != null && locationMap.get("city") != null) {
                return (String) locationMap.get("city");
            }
        } catch (Exception e) {
            log.error("[WxLoginStrategy] 获取用户城市失败", e);
        }
        return "未知城市";
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
