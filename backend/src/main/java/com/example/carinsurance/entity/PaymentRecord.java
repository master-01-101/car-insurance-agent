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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录。
 *
 * <p>对应数据库表 {@code payment_record}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment_record")
public class PaymentRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 insurance_order.id */
    private Long orderId;

    /** 报价 quote_record.id */
    private Long quoteId;

    /** 核保 underwriting_record.id */
    private Long underwritingId;

    /** 支付编号，唯一 */
    private String paymentNo;

    /** 商户支付单号，唯一（幂等） */
    private String outTradeNo;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 渠道：MOCK/ALIPAY/WECHAT */
    private String channel;

    /** 支付平台 */
    private String provider;

    /** 第三方交易流水号 */
    private String transactionId;

    /** 状态：CREATED/PENDING/SUCCESS/FAILED/CLOSED/REFUNDED */
    private String status;

    /** 支付地址 */
    private String payUrl;

    /** 支付过期时间 */
    private LocalDateTime expiresAt;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    /** 后端查单核验时间 */
    private LocalDateTime verifiedAt;

    /** 回调原文（JSON，脱敏） */
    private String callbackPayload;

    /** 数据来源：DEMO/REAL */
    private String dataSource;

    /** 失败原因 */
    private String errorMessage;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
