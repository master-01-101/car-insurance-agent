package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 核保案件响应（从 underwriting_record 映射）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnderwritingCaseResponse {

    /** 主键 */
    private Long id;

    /** 核保编号 */
    private String caseNo;

    /** 订单 id */
    private Long orderId;

    /** 报价 id */
    private Long quoteId;

    /** 状态：PROCESSING / PASSED / REJECTED / NEED_MATERIAL */
    private String status;

    /** 失败/补件原因码 */
    private String reasonCode;

    /** 可读原因 */
    private String reason;

    /** 核保渠道：MOCK */
    private String provider;

    /** 数据来源：DEMO / REAL */
    private String dataSource;

    /** 提交时间 */
    private LocalDateTime submittedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;
}
