package com.example.carinsurance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 改写已有保险方案请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisePlanRequest {

    /** 要改写的方案 id */
    @NotNull(message = "planId 不能为空")
    private Long planId;

    /** 用户改写指令 */
    @NotBlank(message = "instruction 不能为空")
    @Size(max = 500, message = "instruction 长度不能超过 500")
    private String instruction;

    /** 新增险种 code 列表 */
    private List<String> addCoverages;

    /** 移除险种 code 列表 */
    private List<String> removeCoverages;

    /** 保额调整：coverageCode → 新保额 */
    private Map<String, BigDecimal> sumInsuredAdjustments;
}
