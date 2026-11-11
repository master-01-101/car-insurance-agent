package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息表。
 * 对应表 chat_message。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息编号，唯一 */
    private String msgNo;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 会话内序号，从 1 递增 */
    private Integer seq;

    /** 发送者：USER/ASSISTANT/HUMAN_AGENT/SYSTEM */
    private String senderType;

    /** 消息类型：TEXT/IMAGE/CARD/STATUS/HANDOFF */
    private String messageType;

    /** 文本内容（MEDIUMTEXT） */
    private String content;

    /** 卡片/动作/引用等结构（JSON） */
    private String payloadJson;

    /** 涉及的业务请求 ID */
    private String requestId;

    /** 回复的消息 id */
    private Long replyToMessageId;

    /** 关联方案 insurance_plan.id */
    private Long relatedPlanId;

    /** 处理状态：PROCESSING/COMPLETED/FAILED */
    private String status;

    /** 前端幂等 ID */
    private String clientMsgId;

    /** 发送时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
