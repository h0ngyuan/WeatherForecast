package com.wf.aop;

import com.wf.mapper.CityInfoMapper;
import com.wf.mapper.ReminderTaskMapper;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.CityInfoEntity;
import com.wf.object.entity.ReminderTaskEntity;
import com.wf.object.entity.UserInfoEntity;
import com.wf.utils.LocationUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户注册 AOP
 *
 * 监听用户注册，自动为该用户创建一级灾害提醒任务
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class UserRegistrationAspect {

    private final ReminderTaskMapper reminderTaskMapper;
    private final UserInfoMapper userInfoMapper;
    private final CityInfoMapper cityInfoMapper;

    public UserRegistrationAspect(ReminderTaskMapper reminderTaskMapper, UserInfoMapper userInfoMapper, CityInfoMapper cityInfoMapper) {
        this.reminderTaskMapper = reminderTaskMapper;
        this.userInfoMapper = userInfoMapper;
        this.cityInfoMapper = cityInfoMapper;
    }

    /**
     * 切入点：UserInfoMapper.insert 方法
     * 用于监听新用户注册
     */
    @Pointcut("execution(* com.wf.mapper.UserInfoMapper.insert(..))")
    public void userInsertPointcut() {
    }

    /**
     * 用户注册后自动创建一级灾害提醒任务
     *
     * @param joinPoint 连接点
     * @param result    插入结果（影响行数）
     */
    @AfterReturning(value = "userInsertPointcut()", returning = "result")
    public void createDefaultDisasterTask(JoinPoint joinPoint, Integer result) {
        try {
            // 检查插入是否成功
            if (result == null || result <= 0) {
                return;
            }

            // 获取插入的用户实体
            Object[] args = joinPoint.getArgs();
            if (args.length == 0 || !(args[0] instanceof UserInfoEntity)) {
                return;
            }

            UserInfoEntity user = (UserInfoEntity) args[0];
            Long userId = user.getId();

            if (userId == null) {
                log.warn("[UserRegistrationAspect] 用户ID为空，无法创建默认灾害任务");
                return;
            }

            log.info("[UserRegistrationAspect] 检测到新用户注册，userId: {}，准备创建默认一级灾害任务", userId);

            // 获取用户IP对应的城市信息（包含经纬度）
            Map<String, Object> locationInfo = getLocationInfoFromIp();
            String city = (String) locationInfo.getOrDefault("city", "未知城市");
            Double latitude = (Double) locationInfo.get("lat");
            Double longitude = (Double) locationInfo.get("lon");

            if (city == null || city.isEmpty()) {
                city = "未知城市";
                log.warn("[UserRegistrationAspect] 无法获取用户IP对应的城市，使用默认值");
            }

            // 检查并插入城市信息到 CITY_INFO 表
            ensureCityInfoExists(city, latitude, longitude);

            // 创建一级灾害提醒任务
            ReminderTaskEntity task = new ReminderTaskEntity();
            task.setUserId(userId);
            task.setOriginalQuestion("系统自动创建：一级灾害预警");
            task.setConcernWord("一级灾害");
            task.setConcernCondition(null); // 一级灾害不关注具体天气码
            task.setTaskType(1); // 1=总是
            task.setNotifyCondition("一级灾害自动提醒");
            task.setLocation(city);
            task.setTaskStatus(0); // 0=未执行
            task.setExpectedEarliestTime(LocalDateTime.now());
            task.setNotifyByEmail(1); // 默认开启邮件通知
            task.setNotifyBySms(0);
            task.setNotifyByWechat(0);
            task.setAvailable(1); // 可用
            task.setDisasterLevel(1); // 一级灾害
            task.setExpectedLatestTime(null); // 无最晚时间限制
            task.setAlwaysRemind(1); // 总是提醒

            reminderTaskMapper.insert(task);

            log.info("[UserRegistrationAspect] 成功为用户 {} 创建默认一级灾害任务，城市: {}，任务ID: {}",
                    userId, city, task.getId());

        } catch (Exception e) {
            log.error("[UserRegistrationAspect] 创建默认灾害任务失败", e);
            // 不影响主流程，仅记录日志
        }
    }

    /**
     * 从用户请求IP获取城市
     *
     * @return 城市名称
     */
    private String getCityFromUserIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();
            String ip = getClientIp(request);

            // 使用 LocationUtils 根据IP获取城市
            return LocationUtils.getCityByIp(ip);
        } catch (Exception e) {
            log.error("[UserRegistrationAspect] 获取用户IP城市失败", e);
            return null;
        }
    }

    /**
     * 获取客户端真实IP
     *
     * @param request HTTP请求
     * @return IP地址
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

        // 多个代理情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 从用户请求IP获取城市信息（包含经纬度）
     *
     * @return 包含city、lat、lon的Map
     */
    private Map<String, Object> getLocationInfoFromIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("[UserRegistrationAspect] 无法获取请求属性");
                return LocationUtils.getDefaultGpsInfo();
            }

            HttpServletRequest request = attributes.getRequest();
            String ip = getClientIp(request);

            // 使用 LocationUtils 获取完整位置信息
            return LocationUtils.getLocationMapByIp(ip);
        } catch (Exception e) {
            log.error("[UserRegistrationAspect] 获取用户IP位置信息失败", e);
            return LocationUtils.getDefaultGpsInfo();
        }
    }

    /**
     * 确保城市信息存在于 CITY_INFO 表中，不存在则插入
     *
     * @param cityName  城市名称
     * @param latitude  纬度
     * @param longitude 经度
     */
    private void ensureCityInfoExists(String cityName, Double latitude, Double longitude) {
        try {
            if (cityName == null || cityName.isEmpty() || "未知城市".equals(cityName)) {
                log.warn("[UserRegistrationAspect] 城市名称为空或未知，跳过CITY_INFO检查");
                return;
            }

            // 查询城市是否已存在
            CityInfoEntity existingCity = cityInfoMapper.selectByCityName(cityName);
            if (existingCity != null) {
                log.info("[UserRegistrationAspect] 城市 {} 已存在于CITY_INFO表中", cityName);
                return;
            }

            // 城市不存在，插入新记录
            CityInfoEntity newCity = new CityInfoEntity();
            newCity.setCityName(cityName);
            newCity.setCityCode(null); // 暂时为空，后续可补充
            newCity.setLatitude(latitude != null ? BigDecimal.valueOf(latitude) : null);
            newCity.setLongitude(longitude != null ? BigDecimal.valueOf(longitude) : null);
            newCity.setProvince(null); // IP接口通常不返回省份，后续可补充
            newCity.setDistrict(null);
            newCity.setCityLevel(3); // 默认为地级市
            newCity.setTimezone("Asia/Shanghai");
            newCity.setAvailable(1);
            newCity.setIsHot(0); // 非热门城市
            newCity.setDescription("用户注册时自动添加的城市");

            cityInfoMapper.insert(newCity);
            log.info("[UserRegistrationAspect] 成功插入新城市到CITY_INFO表: {}, 经纬度: ({}, {})",
                    cityName, latitude, longitude);

        } catch (Exception e) {
            log.error("[UserRegistrationAspect] 检查/插入城市信息失败: {}", cityName, e);
            // 不影响主流程
        }
    }
}
