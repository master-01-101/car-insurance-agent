package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 保单响应（从 policy + policy_item 映射）。
 *
 * <p>敏感字段（姓名、证件号）均已脱敏，严禁暴露明文。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {

    /** 主键 */
    private Long id;

    /** 保单号 */
    private String policyNo;

    /** 订单 id */
    private Long orderId;

    /** 类型：COMPULSORY / COMMERCIAL */
    private String policyType;

    /** 保险公司编码 */
    private String insurerCode;

    /** 保险公司名称 */
    private String insurerName;

    /** 状态：ISSUING / ISSUED / CANCELLED / EXPIRED */
    private String status;

    /** 保障开始时间 */
    private LocalDateTime effectiveFrom;

    /** 保障结束时间 */
    private LocalDateTime effectiveTo;

    /** 保单总保费（元） */
    private BigDecimal totalPremium;

    /** 币种 */
    private String currency;

    /** 投保人姓名（脱敏） */
    private String applicantNameMasked;

    /** 投保人证件号（脱敏） */
    private String applicantIdMasked;

    /** 被保人姓名（脱敏） */
    private String insuredNameMasked;

    /** 被保人证件号（脱敏） */
    private String insuredIdMasked;

    /** 出单时间 */
    private LocalDateTime issuedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 险种明细 */
    private List<PolicyItemResponse> items;
}
