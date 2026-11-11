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
 * 保险方案（只追加，版本化）。
 *
 * <p>对应数据库表 {@code insurance_plan}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("insurance_plan")
public class InsurancePlan {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 方案对外编号 */
    private String planNo;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 订单 insurance_order.id，可空 */
    private Long orderId;

    /** 上一版本方案 id */
    private Long previousPlanId;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 保险公司编码 */
    private String insurerCode;

    /** 保险公司名称 */
    private String insurerName;

    /** 来源：RECOMMEND/HISTORY/MODIFY/TEMPLATE */
    private String source;

    /** 状态：DRAFT/PENDING_CONFIRMATION/CONFIRMED/SUPERSEDED/CANCELLED */
    private String status;

    /** 预估总保费（元） */
    private BigDecimal estimatedTotal;

    /** 币种 */
    private String currency;

    /** 价格来源：ESTIMATE/INSURER_QUOTE/PENDING_REQUOTE */
    private String priceSource;

    /** 用户指令 */
    private String instruction;

    /** 相对上一版本的变化明细（JSON） */
    private String changedJson;

    /** 确认时间 */
    private LocalDateTime confirmedAt;

    /** 确认人 sys_user.id */
    private Long confirmedBy;

    /** 创建人/服务 */
    private Long createdBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
