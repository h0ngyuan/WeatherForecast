package com.wf.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.mapper.UserInfoMapper;
import com.wf.object.entity.UserInfoEntity;
import com.wf.object.request.WeatherPermissionRequest;
import com.wf.object.response.WeatherAskResponse;
import com.wf.service.ContactService;
import com.wf.service.WeatherGraphOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherGraphOrchestratorImpl implements WeatherGraphOrchestrator {

    private final CompiledGraph weatherGraph;
    private final UserInfoMapper userInfoMapper;
    private final ContactService contactService;

    @Override
    public WeatherAskResponse process(String question) {
        log.info("========== WeatherGraph 开始处理 ==========");
        log.info("用户问题: {}", question);

        Map<String, Object> initialState = Map.of(
                WeatherGraphConstants.KEY_QUESTION, question,
                WeatherGraphConstants.KEY_LOOP_COUNT, 1
        );

        log.info("初始化状态完成，开始调用 graph");
        String threadId = "weather-thread-" + System.currentTimeMillis();
        RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
        Optional<OverAllState> result = weatherGraph.invoke(initialState, config);

        if (result.isEmpty()) {
            log.error("Graph 执行失败，返回空结果");
            return new WeatherAskResponse("系统内部错误", false, 0.0, 0.0, 0);
        }

        OverAllState state = result.get();
        String answer = state.value(WeatherGraphConstants.KEY_ANSWER, "");
        Double relevanceScore = state.value(WeatherGraphConstants.KEY_RELEVANCE_SCORE, 0.0);
        Double qualityScore = state.value(WeatherGraphConstants.KEY_QUALITY_SCORE, 0.0);
        Integer loopCount = state.value(WeatherGraphConstants.KEY_LOOP_COUNT, 1);

        log.info("相关性评分: {}", relevanceScore);
        log.info("质量评分: {}, 循环次数: {}", qualityScore, loopCount);
        log.info("最终答案: {}", answer);
        log.info("========== WeatherGraph 处理完成 ==========");

        return new WeatherAskResponse(answer, true, relevanceScore, qualityScore, loopCount);
    }

    @Override
    public WeatherAskResponse processWithThread(String question, Long userId) {
        log.info("========== WeatherGraph 开始处理(带Thread) ==========");
        log.info("用户问题: {}, 用户ID: {}", question, userId);

        String threadId = "weather-" + UUID.randomUUID().toString();
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(WeatherGraphConstants.KEY_QUESTION, question);
        initialState.put(WeatherGraphConstants.KEY_USER_ID, userId);
        initialState.put(WeatherGraphConstants.KEY_LOOP_COUNT, 1);

        log.info("初始化状态完成，threadId: {}", threadId);
        RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
        Optional<OverAllState> result = weatherGraph.invoke(initialState, config);

        if (result.isEmpty()) {
            log.error("Graph 执行失败，返回空结果");
            return new WeatherAskResponse("系统内部错误", false, 0.0, 0.0, 0);
        }

        OverAllState state = result.get();
        return buildResponse(state, threadId);
    }

    private WeatherAskResponse buildResponse(OverAllState state, String threadId) {
        // 检查是否中断等待权限
        Boolean needIntervention = state.value(WeatherGraphConstants.KEY_NEED_INTERVENTION, false);
        Boolean hasPermission = state.value(WeatherGraphConstants.KEY_HAS_PERMISSION, false);
        
        if (needIntervention && !hasPermission) {
            log.info("流程中断，需要权限授权, threadId: {}", threadId);
            // 返回 NEED_PERMISSION 和 threadId，前端用 | 分隔解析
            return new WeatherAskResponse("NEED_PERMISSION|" + threadId, false, 0.0, 0.0, 0);
        }

        String answer = state.value(WeatherGraphConstants.KEY_ANSWER, "");
        Double relevanceScore = state.value(WeatherGraphConstants.KEY_RELEVANCE_SCORE, 0.0);
        Double qualityScore = state.value(WeatherGraphConstants.KEY_QUALITY_SCORE, 0.0);
        Integer loopCount = state.value(WeatherGraphConstants.KEY_LOOP_COUNT, 1);

        log.info("流程执行完成, threadId: {}", threadId);
        return new WeatherAskResponse(answer, true, relevanceScore, qualityScore, loopCount);
    }

    @Override
    @Transactional
    public void grantPermission(String threadId, Long userId, WeatherPermissionRequest request) {
        log.info("========== 用户授权 ==========");
        log.info("threadId: {}, userId: {}", threadId, userId);

        // 1. 校验用户身份
        StateSnapshot stateSnapshot = weatherGraph.getState(RunnableConfig.builder().threadId(threadId).build());
        if (stateSnapshot == null) {
            log.error("找不到流程状态，threadId: {}", threadId);
            throw new RuntimeException("流程已过期或不存在");
        }

        OverAllState state = stateSnapshot.state();
        Long stateUserId = state.value(WeatherGraphConstants.KEY_USER_ID, 0L);
        if (!stateUserId.equals(userId)) {
            log.error("用户ID不匹配，stateUserId={}, requestUserId={}", stateUserId, userId);
            throw new RuntimeException("无权操作此流程");
        }

        // 2. 更新用户权限设置
        updateUserPermissions(userId, request);

        // 3. 更新用户联系方式
        updateUserContact(userId, request);

        log.info("========== 用户授权完成 ==========");
    }

    @Override
    public WeatherAskResponse resume(String threadId, Long userId) {
        log.info("========== 恢复流程 ==========");
        log.info("threadId: {}, userId: {}", threadId, userId);

        // 1. 校验用户身份
        StateSnapshot stateSnapshot = weatherGraph.getState(RunnableConfig.builder().threadId(threadId).build());
        if (stateSnapshot == null) {
            log.error("找不到流程状态，threadId: {}", threadId);
            return new WeatherAskResponse("流程已过期或不存在", false, 0.0, 0.0, 0);
        }

        OverAllState state = stateSnapshot.state();
        Long stateUserId = state.value(WeatherGraphConstants.KEY_USER_ID, 0L);
        if (!stateUserId.equals(userId)) {
            log.error("用户ID不匹配，stateUserId={}, requestUserId={}", stateUserId, userId);
            return new WeatherAskResponse("无权操作此流程", false, 0.0, 0.0, 0);
        }

        // 2. 恢复流程 - 使用 withResume 从断点继续
        Map<String, Object> updateState = new HashMap<>();
        updateState.put(WeatherGraphConstants.KEY_HUMAN_FEEDBACK, true);
        updateState.put(WeatherGraphConstants.KEY_HAS_PERMISSION, true);

        RunnableConfig config = RunnableConfig.builder()
            .threadId(threadId)
            .resume()
            .build();
        Optional<OverAllState> result = weatherGraph.invoke(updateState, config);

        if (result.isEmpty()) {
            log.error("Graph 恢复执行失败");
            return new WeatherAskResponse("恢复流程失败", false, 0.0, 0.0, 0);
        }

        OverAllState finalState = result.get();
        log.info("========== 恢复流程完成 ==========");
        return buildResponse(finalState, threadId);
    }

    @Override
    public WeatherAskResponse rejectPermission(String threadId, Long userId) {
        log.info("========== 用户拒绝授权 ==========");
        log.info("threadId: {}, userId: {}", threadId, userId);

        // 1. 校验用户身份
        StateSnapshot stateSnapshot = weatherGraph.getState(RunnableConfig.builder().threadId(threadId).build());
        if (stateSnapshot == null) {
            log.error("找不到流程状态，threadId: {}", threadId);
            return new WeatherAskResponse("流程已过期或不存在", false, 0.0, 0.0, 0);
        }

        OverAllState state = stateSnapshot.state();
        Long stateUserId = state.value(WeatherGraphConstants.KEY_USER_ID, 0L);
        if (!stateUserId.equals(userId)) {
            log.error("用户ID不匹配，stateUserId={}, requestUserId={}", stateUserId, userId);
            return new WeatherAskResponse("无权操作此流程", false, 0.0, 0.0, 0);
        }

        // 2. 设置 humanFeedback 为 false 表示用户拒绝 - 使用 withResume 从断点继续
        Map<String, Object> updateState = new HashMap<>();
        updateState.put(WeatherGraphConstants.KEY_HUMAN_FEEDBACK, false);

        RunnableConfig config = RunnableConfig.builder()
            .threadId(threadId)
            .resume()
            .build();
        Optional<OverAllState> result = weatherGraph.invoke(updateState, config);

        if (result.isEmpty()) {
            log.error("Graph 执行失败");
            return new WeatherAskResponse("流程执行失败", false, 0.0, 0.0, 0);
        }

        OverAllState finalState = result.get();
        log.info("========== 拒绝授权流程完成 ==========");
        return buildResponse(finalState, threadId);
    }

    private void updateUserPermissions(Long userId, WeatherPermissionRequest request) {
        UserInfoEntity userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            log.warn("用户不存在，无法更新权限: userId={}", userId);
            return;
        }

        boolean updated = false;

        if (request.getGrantPhonePermission() != null && request.getGrantPhonePermission()) {
            userInfo.setPhoneNotifyPermission(1);
            updated = true;
            log.info("用户授权手机号通知权限: userId={}", userId);
        }

        if (request.getGrantEmailPermission() != null && request.getGrantEmailPermission()) {
            userInfo.setEmailNotifyPermission(1);
            updated = true;
            log.info("用户授权邮箱通知权限: userId={}", userId);
        }

        if (request.getGrantWechatPermission() != null && request.getGrantWechatPermission()) {
            userInfo.setWechatNotifyPermission(1);
            updated = true;
            log.info("用户授权微信通知权限: userId={}", userId);
        }

        if (updated) {
            userInfoMapper.updateById(userInfo);
            log.info("用户权限更新成功: userId={}", userId);
        }
    }

    private void updateUserContact(Long userId, WeatherPermissionRequest request) {
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            try {
                contactService.bindPhoneDirect(userId, request.getPhone());
                log.info("绑定手机号成功: userId={}, phone={}", userId, request.getPhone());
            } catch (Exception e) {
                log.warn("绑定手机号失败: userId={}, error={}", userId, e.getMessage());
            }
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            try {
                contactService.bindEmailDirect(userId, request.getEmail());
                log.info("绑定邮箱成功: userId={}, email={}", userId, request.getEmail());
            } catch (Exception e) {
                log.warn("绑定邮箱失败: userId={}, error={}", userId, e.getMessage());
            }
        }
    }
}
