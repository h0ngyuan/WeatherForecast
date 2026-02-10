package com.wf.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wf.object.entity.ChatSessionEntity;
import com.wf.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springblade.core.tool.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "聊天会话", description = "聊天会话管理接口")
@RestController
@RequestMapping("/chat/session")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @Operation(summary = "创建会话", description = "创建新的聊天会话")
    @PostMapping("/create")
    public R<ChatSessionEntity> createSession(
            @Parameter(description = "会话标题") @RequestParam(required = false) String sessionTitle) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        ChatSessionEntity session = chatSessionService.createSession(userId, sessionTitle);
        return R.data(session);
    }

    @Operation(summary = "获取会话详情", description = "根据会话ID获取会话详情")
    @GetMapping("/get")
    public R<ChatSessionEntity> getSessionById(
            @Parameter(description = "会话ID") @RequestParam Long sessionId) {
        ChatSessionEntity session = chatSessionService.getSessionById(sessionId);
        return R.data(session);
    }

    @Operation(summary = "更新会话标题", description = "更新会话的标题")
    @PostMapping("/updateTitle")
    public R<String> updateSessionTitle(
            @Parameter(description = "会话ID") @RequestParam Long sessionId,
            @Parameter(description = "新标题") @RequestParam String title) {
        chatSessionService.updateSessionTitle(sessionId, title);
        return R.success("更新成功");
    }

    @Operation(summary = "结束会话", description = "结束指定的会话")
    @PostMapping("/end")
    public R<String> endSession(
            @Parameter(description = "会话ID") @RequestParam Long sessionId) {
        chatSessionService.endSession(sessionId);
        return R.success("结束成功");
    }
}
