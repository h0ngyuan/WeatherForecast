package com.wf.job;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.agent.entity.DisasterInfo;
import com.wf.agent.graph.node.AlertTextGenerationNode;
import com.wf.mapper.CityInfoMapper;
import com.wf.mapper.ReminderTaskMapper;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.CityInfoEntity;
import com.wf.object.entity.ReminderTaskEntity;
import com.wf.object.entity.UserInfoEntity;
import com.wf.service.EmailNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 天气紧急响应定时任务
 *
 * 职责：
 * 每日定时执行，协调多Agent协作评估灾害，生成预警文本并通知用户
 *
 * 新架构 - EmergencyResponseGraph 编排4个Node：
 * 1. WeatherPredictionNode: MCP查询24小时天气码
 * 2. DisasterAssessmentNode: AI识别灾害类型和时间段
 * 3. DisasterLevelAssessmentNode: AI+Skill评判风险等级
 * 4. AlertTextGenerationNode: 生成预警文本
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
public class WeatherEmergencyJob {

    private final CityInfoMapper cityInfoMapper;
    private final CompiledGraph emergencyResponseGraph;
    private final AlertTextGenerationNode alertTextGenerationNode;
    private final ReminderTaskMapper reminderTaskMapper;
    private final UserInfoMapper userInfoMapper;
    private final EmailNotificationService emailNotificationService;

    @Autowired
    public WeatherEmergencyJob(
            CityInfoMapper cityInfoMapper,
            @Qualifier("emergencyResponseGraph") CompiledGraph emergencyResponseGraph,
            AlertTextGenerationNode alertTextGenerationNode,
            ReminderTaskMapper reminderTaskMapper,
            UserInfoMapper userInfoMapper,
            EmailNotificationService emailNotificationService) {
        this.cityInfoMapper = cityInfoMapper;
        this.emergencyResponseGraph = emergencyResponseGraph;
        this.alertTextGenerationNode = alertTextGenerationNode;
        this.reminderTaskMapper = reminderTaskMapper;
        this.userInfoMapper = userInfoMapper;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * 每日天气检查任务
     *
     * 执行流程：
     * 1. 获取所有监控地区
     * 2. 遍历每个地区
     * 3. 【Graph】EmergencyResponseGraph: 编排4个Node执行完整流程
     *    - weatherPrediction: MCP查询24小时天气码
     *    - disasterAssessment: AI识别灾害
     *    - disasterLevelAssessment: AI+Skill评判等级
     *    - alertTextGeneration: 生成预警文本
     * 4. 根据灾害级别分流通知
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void dailyCheck() {
        log.info("========== [紧急响应] 每日天气检查任务开始 ==========");

        try {
            // 从 CITY_INFO 表获取所有可用城市
            List<CityInfoEntity> cities = cityInfoMapper.selectList(
                    new LambdaQueryWrapper<CityInfoEntity>()
                            .eq(CityInfoEntity::getAvailable, 1));
            log.info("[紧急响应] 获取到 {} 个监控城市", cities.size());

            for (CityInfoEntity city : cities) {
                String location = city.getCityName();
                log.info("[紧急响应] 处理城市: {}", location);

                try {
                    // ========== Step 1: 获取经纬度 ==========
                    double[] coordinates = getCoordinates(city);
                    if (coordinates == null) {
                        log.warn("[紧急响应] 无法获取 {} 的经纬度，跳过", location);
                        continue;
                    }

                    // ========== Step 2: 【Graph】执行完整紧急响应流程 ==========
                    log.info("[紧急响应] [Graph] 执行紧急响应流程...");
                    
                    Map<String, Object> initialState = new HashMap<>();
                    initialState.put("location", location);
                    initialState.put("latitude", coordinates[0]);
                    initialState.put("longitude", coordinates[1]);

                    Optional<OverAllState> result = emergencyResponseGraph.invoke(initialState);
                    
                    if (result.isEmpty()) {
                        log.error("[紧急响应] Graph 执行失败，返回空结果");
                        continue;
                    }

                    OverAllState finalState = result.get();
                    
                    @SuppressWarnings("unchecked")
                    List<Integer> weatherCodes = finalState.value("weatherCodes", List.of());
                    @SuppressWarnings("unchecked")
                    List<DisasterInfo> confirmedDisasters = finalState.value("confirmedDisasters", List.of());
                    String alertText = finalState.value("alertText", "");

                    log.info("[紧急响应] [Graph] 流程执行完成，获取到 {} 个天气码，{} 个灾害",
                            weatherCodes.size(), confirmedDisasters.size());

                    if (confirmedDisasters.isEmpty()) {
                        log.info("[紧急响应] {} 无灾害", location);
                        continue;
                    }

                    // 打印评估结果
                    for (DisasterInfo d : confirmedDisasters) {
                        log.info("[紧急响应] 灾害: {} | 等级: {} | {}",
                                d.getType(), d.getLevel(), d.getDescription());
                    }

                    // ========== Step 3: 分离一级灾害和其他级别 ==========
                    List<DisasterInfo> level1Disasters = confirmedDisasters.stream()
                            .filter(d -> d.getLevel() == 1)
                            .collect(Collectors.toList());
                    List<DisasterInfo> otherDisasters = confirmedDisasters.stream()
                            .filter(d -> d.getLevel() > 1)
                            .collect(Collectors.toList());

                    // ========== Step 4: 一级灾害 - 全员通知 ==========
                    if (!level1Disasters.isEmpty()) {
                        log.info("[紧急响应] {} 发生一级灾害，通知所有用户", location);

                        // 【Node】生成一级灾害预警文本
                        String level1AlertText = alertTextGenerationNode.generateLevel1Alert(location, level1Disasters);

                        // 查询该地区的一级灾害提醒任务（alwaysRemind=1）
                        List<ReminderTaskEntity> tasks = reminderTaskMapper.selectLevel1TasksByLocation(location);
                        int notifyCount = 0;

                        for (ReminderTaskEntity task : tasks) {
                            // 查询用户信息
                            UserInfoEntity user = userInfoMapper.selectById(task.getUserId());
                            if (user != null && user.getEmailNotifyPermission() == 1) {
                                emailNotificationService.sendDisasterAlertWithText(
                                        user.getEmail(), location, level1AlertText);
                                notifyCount++;
                            }
                        }
                        log.info("[紧急响应] 已向 {} 个用户发送一级灾害预警", notifyCount);
                    }

                    // ========== Step 5: 二/三级灾害 - 精准通知 ==========
                    LocalDateTime now = LocalDateTime.now();
                    for (DisasterInfo disaster : otherDisasters) {
                        log.info("[紧急响应] {} 发生{}级灾害：{}，精准通知",
                                location, disaster.getLevel(), disaster.getType());

                        // 查询该地区和天气码的二三级灾害提醒任务
                        List<ReminderTaskEntity> tasks = reminderTaskMapper
                                .selectLevel2And3TasksByLocationAndWeatherCode(
                                        location, disaster.getWeatherCode(), now);
                        int notifyCount = 0;

                        for (ReminderTaskEntity task : tasks) {
                            UserInfoEntity user = userInfoMapper.selectById(task.getUserId());
                            if (user != null && user.getEmailNotifyPermission() == 1) {
                                // 【Node】生成个性化提醒
                                String concernWord = task.getConcernWord() != null ?
                                        task.getConcernWord() : "相关活动";
                                String reminderText = alertTextGenerationNode
                                        .generateLevel2Alert(location, disaster, concernWord);

                                // 发送邮件
                                emailNotificationService.sendReminderWithText(
                                        user.getEmail(), location, reminderText);
                                notifyCount++;

                                // 如果不是总是提醒，通知后设置 available=0
                                if (task.getAlwaysRemind() == null || task.getAlwaysRemind() == 0) {
                                    reminderTaskMapper.updateAvailable(task.getId(), 0);
                                    log.debug("[紧急响应] 任务 {} 已标记为不可用", task.getId());
                                }
                            }
                        }
                        log.info("[紧急响应] 已向 {} 个用户发送精准提醒", notifyCount);
                    }

                } catch (Exception e) {
                    log.error("[紧急响应] 处理地区 {} 失败", location, e);
                }
            }

        } catch (Exception e) {
            log.error("[紧急响应] 每日天气检查任务执行失败", e);
        }

        log.info("========== [紧急响应] 每日天气检查任务完成 ==========");
    }

    /**
     * 从 CityInfoEntity 获取经纬度
     */
    private double[] getCoordinates(CityInfoEntity city) {
        if (city == null) {
            log.warn("[紧急响应] 城市信息为空");
            return null;
        }

        if (city.getLatitude() == null || city.getLongitude() == null) {
            log.warn("[紧急响应] 城市 {} 的经纬度信息不完整", city.getCityName());
            return null;
        }

        return new double[]{city.getLatitude().doubleValue(), city.getLongitude().doubleValue()};
    }
}
