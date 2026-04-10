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
@TableName("CHAT_SESSION_MESSAGE")
@Schema(description = "聊天记录表")
public class ChatHistoryEntity implements Serializable {

    @Schema(description = "记录ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "会话ID，关联CHAT_SESSION.ID")
    private Long chatSessionId;

    @Schema(description = "用户ID，关联USER_INFO.ID")
    private Long userId;

    @Schema(description = "发送者类型：0=用户，1=AI助手")
    private Integer senderType;

    @Schema(description = "消息内容")
    private String messageContent;

    @Schema(description = "消息类型：0=文本，1=图片，2=文件，3=语音，4=视频")
    private Integer messageType;

    @Schema(description = "消息状态：0=已发送，1=已读，2=发送失败")
    private Integer messageStatus;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    @Schema(description = "是否可用：1=可用，0=不可用")
    @TableLogic
    private Integer available;

    @Schema(description = "记录创建时间")
    private LocalDateTime createTime;

    @Schema(description = "记录最后更新时间")
    private LocalDateTime updateTime;

    // 兼容方法
    public Long getSessionId() {
        return chatSessionId;
    }

    public void setSessionId(Long sessionId) {
        this.chatSessionId = sessionId;
    }

    public String getRole() {
        return senderType != null && senderType == 1 ? "assistant" : "user";
    }

    public void setRole(String role) {
        this.senderType = "assistant".equals(role) ? 1 : 0;
    }

    public String getContent() {
        return messageContent;
    }

    public void setContent(String content) {
        this.messageContent = content;
    }
}
