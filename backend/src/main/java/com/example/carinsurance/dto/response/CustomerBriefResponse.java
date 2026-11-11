package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户摘要（工作台客户列表项）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBriefResponse {

    /** 会话 id */
    private Long conversationId;

    /** 会话标题 */
    private String conversationTitle;

    /** 客户姓名（脱敏） */
    private String customerNameMasked;

    /** 客户手机号（脱敏） */
    private String customerPhoneMasked;

    /** 当前阶段 */
    private String stage;

    /** 最近一条消息时间 */
    private LocalDateTime lastMessageAt;
}
