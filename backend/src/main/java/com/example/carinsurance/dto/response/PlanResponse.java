package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 保险方案响应（从 insurance_plan + insurance_plan_item 映射）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {

    /** 主键 */
    private Long id;

    /** 方案对外编号 */
    private String planNo;

    /** 会话 id */
    private Long conversationId;

    /** 乐观锁版本号 */
    private Integer version;

    /** 上一版本方案 id */
    private Long previousPlanId;

    /** 保险公司编码 */
    private String insurerCode;

    /** 保险公司名称 */
    private String insurerName;

    /** 来源：RECOMMEND / HISTORY / MODIFY / TEMPLATE */
    private String source;

    /** 状态 */
    private String status;

    /** 预估总保费（元） */
    private BigDecimal estimatedTotal;

    /** 币种 */
    private String currency;

    /** 价格来源：DEMO / ESTIMATE / INSURER_QUOTE / PENDING_REQUOTE */
    private String priceSource;

    /** 用户指令 */
    private String instruction;

    /** 确认时间 */
    private LocalDateTime confirmedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 险种明细 */
    private List<PlanItemResponse> items;
}
