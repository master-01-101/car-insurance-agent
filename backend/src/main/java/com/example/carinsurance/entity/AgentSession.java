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
 * 会话与 LangGraph 线程映射。
 *
 * <p>对应数据库表 {@code agent_session}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_session")
public class AgentSession {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 conversation.id */
    private Long conversationId;

    /** LangGraph thread_id，唯一 */
    private String threadId;

    /** 图版本 */
    private String graphVersion;

    /** 最近 checkpoint ID */
    private String lastCheckpointId;

    /** 状态：ACTIVE/CLOSED */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
