package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 人工接管工单详情响应（从 handoff_task 映射）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoffTicketDetailResponse {

    /** 主键 */
    private Long id;

    /** 工单编号 */
    private String ticketNo;

    /** 会话 id */
    private Long conversationId;

    /** 客户（脱敏） */
    private String customerMasked;

    /** 原因码 */
    private String reasonCode;

    /** 原因描述 */
    private String reason;

    /** 交接摘要 */
    private String summary;

    /** 状态：PENDING / ACCEPTED / RESOLVED / CANCELLED */
    private String status;

    /** 排队位次 */
    private Integer queuePosition;

    /** 处理业务员 id */
    private Long assignedAgentId;

    /** 处理结果 */
    private String resolution;

    /** 发起时间 */
    private LocalDateTime requestedAt;

    /** 领取时间 */
    private LocalDateTime acceptedAt;

    /** 解决时间 */
    private LocalDateTime resolvedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 事件时间线 */
    private List<HandoffEventResponse> events;
}
