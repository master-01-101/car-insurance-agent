package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 人工接管工单（表 handoff_task，含 version 乐观锁）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("handoff_task")
public class HandoffTask {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号，唯一 */
    private String ticketNo;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 订单 insurance_order.id */
    private Long orderId;

    /** 客户 sys_user.id */
    private Long customerId;

    /** 原因码：OCR_CONFLICT / USER_REQUEST / BUSINESS_FAILURE */
    private String reasonCode;

    /** 原因描述 */
    private String reason;

    /** 交接摘要 */
    private String summary;

    /** 状态：PENDING / ACCEPTED / RESOLVED / CANCELLED */
    private String status;

    /** 排队位次 */
    private Integer queuePosition;

    /** 处理业务员 sys_user.id */
    private Long assignedAgentId;

    /** 处理结果 */
    private String resolution;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 发起时间 */
    private LocalDateTime requestedAt;

    /** 领取时间 */
    private LocalDateTime acceptedAt;

    /** 解决时间 */
    private LocalDateTime resolvedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
