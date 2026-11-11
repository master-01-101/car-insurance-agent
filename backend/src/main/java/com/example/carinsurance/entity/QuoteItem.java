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
 * 报价险种明细。
 *
 * <p>对应数据库表 {@code quote_item}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("quote_item")
public class QuoteItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报价 quote_record.id */
    private Long quoteId;

    /** 险种编码 */
    private String coverageCode;

    /** 险种名称 */
    private String coverageName;

    /** 保额（元） */
    private BigDecimal sumInsured;

    /** 保费（元） */
    private BigDecimal premium;

    /** 展示排序 */
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
