package com.wf.service;

import com.wf.object.entity.ChatHistoryEntity;

import java.util.List;

public interface ChatHistoryService {

    /**
     * 保存聊天记录
     */
    void saveMessage(ChatHistoryEntity message);

    /**
     * 获取会话最近的N条记录（用于AI上下文）
     */
    List<ChatHistoryEntity> getRecentMessages(Long sessionId, int limit);

    /**
     * 获取会话的所有聊天记录
     */
    List<ChatHistoryEntity> getSessionMessages(Long sessionId);



    /**
     * 创建新会话
     */
    Long createSession(Long userId, String title);

    /**
     * 获取或创建用户当前会话
     */
    Long getOrCreateCurrentSession(Long userId);
}
