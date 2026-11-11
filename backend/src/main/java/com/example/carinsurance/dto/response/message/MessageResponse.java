package com.example.carinsurance.dto.response.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息响应。
 * 对应 entity ChatMessage（精简字段，不含内部状态 payloadJson 等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    /** 主键 */
    private Long id;

    /** 消息编号（对外编号） */
    private String msgNo;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 会话内序号 */
    private Integer seq;

    /** 发送者类型：USER/ASSISTANT/HUMAN_AGENT/SYSTEM */
    private String senderType;

    /** 消息类型：TEXT/IMAGE/CARD/STATUS/HANDOFF */
    private String messageType;

    /** 消息文本内容 */
    private String content;

    /** 发送时间 */
    private LocalDateTime createdAt;
}
