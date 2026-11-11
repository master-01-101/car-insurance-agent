package com.example.carinsurance.dto.response.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话详情/列表项响应。
 * 对应 entity Conversation。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    /** 主键 */
    private Long id;

    /** 会话编号（对外编号） */
    private String convNo;

    /** 客户 sys_user.id */
    private Long customerId;

    /** 业务员 sys_user.id */
    private Long salesmanId;

    /** 车辆 vehicle.id */
    private Long vehicleId;

    /** 车牌号 */
    private String plateNumber;

    /** 会话标题 */
    private String title;

    /** 业务阶段 */
    private String stage;

    /** 处理方式：AI/HUMAN */
    private String handlingMode;

    /** 状态：ACTIVE/HANDOFF/CLOSED/ABORTED */
    private String status;

    /** 最后一条消息时间 */
    private LocalDateTime lastMessageAt;

    /** 消息数量 */
    private Integer messageCount;

    /** 关闭时间 */
    private LocalDateTime closedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
