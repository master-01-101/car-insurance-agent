package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保单（一单可多张）。
 *
 * <p>对应数据库表 {@code policy}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("policy")
public class Policy {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 insurance_order.id */
    private Long orderId;

    /** 车辆 vehicle.id */
    private Long vehicleId;

    /** 正式保单号，唯一 */
    private String policyNo;

    /** 类型：COMPULSORY/COMMERCIAL */
    private String policyType;

    /** 保险公司编码 */
    private String insurerCode;

    /** 保险公司名称 */
    private String insurerName;

    /** 状态：ISSUING/ISSUED/CANCELLED/EXPIRED */
    private String status;

    /** 保障开始时间 */
    private LocalDateTime effectiveFrom;

    /** 保障结束时间 */
    private LocalDateTime effectiveTo;

    /** 保单总保费（元） */
    private BigDecimal totalPremium;

    /** 币种 */
    private String currency;

    /** 投保人姓名脱敏 */
    private String applicantNameMasked;

    /** 投保人证件号脱敏 */
    private String applicantIdMasked;

    /** 被保人姓名脱敏 */
    private String insuredNameMasked;

    /** 被保人证件号脱敏 */
    private String insuredIdMasked;

    /** 出单时车辆快照（JSON） */
    private String vehicleSnapshot;

    /** 出单时人员快照（JSON） */
    private String partySnapshot;

    /** 条款摘要（JSON） */
    private String terms;

    /** 保单文件 file_object.id */
    private Long documentFileId;

    /** 数据来源：DEMO/REAL */
    private String dataSource;

    /** 出单时间 */
    private LocalDateTime issuedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
