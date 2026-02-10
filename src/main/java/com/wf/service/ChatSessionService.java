package com.wf.service;

import com.wf.object.entity.ChatSessionEntity;

public interface ChatSessionService {
    ChatSessionEntity createSession(Long userId, String sessionTitle);
    ChatSessionEntity getSessionById(Long sessionId);
    void updateSessionTitle(Long sessionId, String title);
    void endSession(Long sessionId);
}
