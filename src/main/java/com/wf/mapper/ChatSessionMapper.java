package com.wf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wf.object.entity.ChatSessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionEntity> {
}
