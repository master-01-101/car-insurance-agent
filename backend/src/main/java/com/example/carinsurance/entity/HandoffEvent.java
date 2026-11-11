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
 * 人工接管事件时间线（表 handoff_event，无 version / 无 updated_at）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("handoff_event")
public class HandoffEvent {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 handoff_task.id */
    private Long handoffId;

    /** 事件类型：REQUESTED / QUEUED / ACCEPTED / MESSAGE / RESOLVED / CANCELLED */
    private String eventType;

    /** 操作者类型：CUSTOMER / SALESMAN / ADMIN / SYSTEM / AI */
    private String actorType;

    /** 操作者 sys_user.id */
    private Long actorId;

    /** 事件载荷（脱敏后的 JSON） */
    private String payloadJson;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
