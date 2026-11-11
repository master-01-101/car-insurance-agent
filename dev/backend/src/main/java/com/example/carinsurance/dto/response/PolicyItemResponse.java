package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 保单险种明细响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyItemResponse {

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
}
