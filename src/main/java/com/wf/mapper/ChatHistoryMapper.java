package com.wf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wf.object.entity.ChatHistoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistoryEntity> {

    /**
     * 获取会话最近的N条聊天记录
     */
    @Select("SELECT * FROM CHAT_SESSION_MESSAGE WHERE chat_session_id = #{sessionId} AND available = 1 " +
            "ORDER BY id DESC LIMIT #{limit}")
    List<ChatHistoryEntity> selectRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    /**
     * 获取会话的所有聊天记录（按时间正序）
     */
    @Select("SELECT * FROM CHAT_SESSION_MESSAGE WHERE chat_session_id = #{sessionId} AND available = 1 " +
            "ORDER BY id ASC")
    List<ChatHistoryEntity> selectBySessionId(@Param("sessionId") Long sessionId);
}
