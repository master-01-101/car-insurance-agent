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
 * 保险订单（聚合根）。
 *
 * <p>对应数据库表 {@code insurance_order}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("insurance_order")
public class InsuranceOrder {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，唯一 */
    private String orderNo;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 客户 sys_user.id */
    private Long customerId;

    /** 业务员 sys_user.id */
    private Long salesmanId;

    /** 车辆 vehicle.id */
    private Long vehicleId;

    /** 已确认方案 insurance_plan.id */
    private Long planId;

    /** 已确认方案版本 */
    private Integer planVersion;

    /** 车辆资料版本 */
    private Integer vehicleVersion;

    /** 各角色人员资料版本（JSON） */
    private String partyVersions;

    /** 订单状态，见状态机 */
    private String status;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 总保费（元） */
    private BigDecimal totalPremium;

    /** 税额（元） */
    private BigDecimal taxAmount;

    /** 应付金额（元） */
    private BigDecimal payableAmount;

    /** 订单失败原因 */
    private String failReason;

    /** 币种 */
    private String currency;

    /** 当前有效报价 quote_record.id */
    private Long activeQuoteId;

    /** 当前有效核保 underwriting_record.id */
    private Long activeUnderwritingId;

    /** 当前支付 payment_record.id */
    private Long activePaymentId;

    /** 下单时车辆快照（JSON） */
    private String vehicleSnapshot;

    /** 下单时人员快照（JSON） */
    private String partySnapshot;

    /** 下单时方案及险种快照（JSON） */
    private String planSnapshot;

    /** 用户明确发起报价时间 */
    private LocalDateTime submittedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
