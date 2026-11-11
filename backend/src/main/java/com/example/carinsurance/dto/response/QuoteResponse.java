package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报价记录响应（从 quote_record + quote_item 映射）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponse {

    /** 主键 */
    private Long id;

    /** 报价编号 */
    private String quoteNo;

    /** 订单 id */
    private Long orderId;

    /** 方案 id */
    private Long planId;

    /** 方案版本 */
    private Integer planVersion;

    /** 保险公司编码 */
    private String insurerCode;

    /** 保险公司名称 */
    private String insurerName;

    /** 状态：PROCESSING / SUCCEEDED / FAILED / EXPIRED */
    private String status;

    /** 总保费（元） */
    private BigDecimal totalPremium;

    /** 税额（元） */
    private BigDecimal taxAmount;

    /** 应付金额（元） */
    private BigDecimal payableAmount;

    /** 币种 */
    private String currency;

    /** 报价有效期 */
    private LocalDateTime validUntil;

    /** 报价渠道：MOCK */
    private String provider;

    /** 数据来源：DEMO / REAL */
    private String dataSource;

    /** 失败原因 */
    private String errorMessage;

    /** 请求时间 */
    private LocalDateTime requestedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 险种明细 */
    private List<QuoteItemResponse> items;
}
