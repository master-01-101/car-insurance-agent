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
 * 保险方案险种明细。
 *
 * <p>对应数据库表 {@code insurance_plan_item}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("insurance_plan_item")
public class InsurancePlanItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 方案 insurance_plan.id */
    private Long planId;

    /** 险种编码 */
    private String coverageCode;

    /** 险种名称 */
    private String coverageName;

    /** 是否选择：1-是 0-否 */
    private Integer selected;

    /** 是否必投（如交强险）：1-是 0-否 */
    private Integer isRequired;

    /** 保额（元） */
    private BigDecimal sumInsured;

    /** 免赔额（元） */
    private BigDecimal deductible;

    /** 预估保费（元） */
    private BigDecimal estimatedPremium;

    /** 条款摘要（JSON） */
    private String terms;

    /** 展示排序 */
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
