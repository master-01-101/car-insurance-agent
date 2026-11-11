package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录响应（从 payment_record 映射）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /** 主键 */
    private Long id;

    /** 支付编号 */
    private String paymentNo;

    /** 订单 id */
    private Long orderId;

    /** 报价 id */
    private Long quoteId;

    /** 支付渠道：MOCK / ALIPAY / WECHAT */
    private String channel;

    /** 状态：CREATED / PENDING / SUCCESS / FAILED / CLOSED / REFUNDED */
    private String status;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 支付地址 */
    private String payUrl;

    /** 第三方交易流水号 */
    private String transactionId;

    /** 支付过期时间 */
    private LocalDateTime expiresAt;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    /** 后端查单核验时间 */
    private LocalDateTime verifiedAt;

    /** 数据来源：DEMO / REAL */
    private String dataSource;

    /** 失败原因 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
