package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 人工接管工单列表项响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoffTicketBriefResponse {

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

    /** 发起时间 */
    private LocalDateTime requestedAt;
}
