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
 * 报价记录。
 *
 * <p>对应数据库表 {@code quote_record}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("quote_record")
public class QuoteRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 insurance_order.id */
    private Long orderId;

    /** 报价编号，唯一 */
    private String quoteNo;

    /** 报价依据方案 insurance_plan.id */
    private Long planId;

    /** 方案版本 */
    private Integer planVersion;

    /** 保险公司编码 */
    private String insurerCode;

    /** 保险公司名称 */
    private String insurerName;

    /** 状态：PROCESSING/SUCCEEDED/FAILED/EXPIRED */
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

    /** 外部报价编号 */
    private String externalQuoteNo;

    /** 报价渠道 */
    private String provider;

    /** 数据来源：DEMO/REAL */
    private String dataSource;

    /** 第三方响应摘要（JSON） */
    private String responseSummary;

    /** 失败错误码 */
    private String errorCode;

    /** 失败原因 */
    private String errorMessage;

    /** 请求时间 */
    private LocalDateTime requestedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

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
