package com.wf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.mapper.ChatSessionMapper;
import com.wf.object.entity.ChatSessionEntity;
import com.wf.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Override
    public ChatSessionEntity createSession(Long userId, String sessionTitle) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setUserId(userId);
        session.setAssistantName("AI");
        session.setSessionTitle(sessionTitle.substring(0,10)+"...");
        session.setSessionStatus(1);
        session.setMessageCount(0);
        session.setAvailable(1);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public ChatSessionEntity getSessionById(Long sessionId) {
        return chatSessionMapper.selectById(sessionId);
    }

    @Override
    public void updateSessionTitle(Long sessionId, String title) {
        ChatSessionEntity session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setSessionTitle(title);
            session.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }

    @Override
    public void endSession(Long sessionId) {
        ChatSessionEntity session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setSessionStatus(0);
            session.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }
}
