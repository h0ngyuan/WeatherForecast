package com.wf.object.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("CHAT_SESSION")
@Schema(description = "聊天会话主表")
public class ChatSessionEntity implements Serializable {

    @Schema(description = "会话ID，从100000开始")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID，关联USER_INFO.ID，可为空")
    private Long userId;

    @Schema(description = "回复者名称，默认为AI")
    private String assistantName;

    @Schema(description = "会话标题，可自动生成或用户设置")
    private String sessionTitle;

    @Schema(description = "会话状态：1=活跃，0=已结束，2=已归档")
    private Integer sessionStatus;

    @Schema(description = "消息总数")
    private Integer messageCount;

    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "是否可用：1=可用，0=不可用")
    @TableLogic
    private Integer available;

    @Schema(description = "会话创建时间")
    private LocalDateTime createTime;

    @Schema(description = "会话最后更新时间")
    private LocalDateTime updateTime;
}
