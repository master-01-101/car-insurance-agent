package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保险方案险种明细响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanItemResponse {

    /** 险种编码 */
    private String coverageCode;

    /** 险种名称 */
    private String coverageName;

    /** 是否选择：1-是 0-否 */
    private Integer selected;

    /** 是否必投：1-是 0-否 */
    private Integer isRequired;

    /** 保额（元） */
    private BigDecimal sumInsured;

    /** 免赔额（元） */
    private BigDecimal deductible;

    /** 预估保费（元） */
    private BigDecimal estimatedPremium;

    /** 展示排序 */
    private Integer sortOrder;
}
