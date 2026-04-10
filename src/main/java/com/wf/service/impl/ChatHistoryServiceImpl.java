package com.wf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.mapper.ChatHistoryMapper;
import com.wf.mapper.ChatSessionMapper;
import com.wf.object.entity.ChatHistoryEntity;
import com.wf.object.entity.ChatSessionEntity;
import com.wf.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String CURRENT_SESSION_KEY = "chat:current_session:";
    private static final long SESSION_EXPIRE_HOURS = 24;

    @Override
    @Transactional
    public void saveMessage(ChatHistoryEntity message) {
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        message.setSendTime(LocalDateTime.now());
        message.setAvailable(1);
        message.setMessageStatus(0);
        chatHistoryMapper.insert(message);

        // 更新会话最后消息时间和消息计数
        ChatSessionEntity session = chatSessionMapper.selectById(message.getSessionId());
        if (session != null) {
            session.setLastMessageTime(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());
            session.setMessageCount((session.getMessageCount() != null ? session.getMessageCount() : 0) + 1);
            chatSessionMapper.updateById(session);
        }
    }

    @Override
    public List<ChatHistoryEntity> getRecentMessages(Long sessionId, int limit) {
        List<ChatHistoryEntity> messages = chatHistoryMapper.selectRecentBySessionId(sessionId, limit);
        // 反转列表，使其按时间正序排列
        java.util.Collections.reverse(messages);
        return messages;
    }

    @Override
    public List<ChatHistoryEntity> getSessionMessages(Long sessionId) {
        return chatHistoryMapper.selectBySessionId(sessionId);
    }

    @Override
    @Transactional
    public Long createSession(Long userId, String title) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setUserId(userId);
        session.setSessionTitle(title != null ? title : "新对话");
        session.setAssistantName("AI天气助手");
        session.setSessionStatus(1);
        session.setMessageCount(0);
        session.setLastMessageTime(LocalDateTime.now());
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setAvailable(1);

        chatSessionMapper.insert(session);

        // 设置为当前会话
        setCurrentSession(userId, session.getId());

        return session.getId();
    }

    @Override
    public Long getOrCreateCurrentSession(Long userId) {
        String key = CURRENT_SESSION_KEY + userId;
        String sessionIdStr = redisTemplate.opsForValue().get(key);

        if (sessionIdStr != null) {
            Long sessionId = Long.valueOf(sessionIdStr);
            // 检查会话是否存在且活跃
            ChatSessionEntity session = chatSessionMapper.selectById(sessionId);
            if (session != null && session.getAvailable() == 1 && session.getSessionStatus() == 1) {
                return sessionId;
            }
        }

        // 创建新会话
        return createSession(userId, "新对话");
    }

    private void setCurrentSession(Long userId, Long sessionId) {
        String key = CURRENT_SESSION_KEY + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(sessionId), SESSION_EXPIRE_HOURS, TimeUnit.HOURS);
    }
}
