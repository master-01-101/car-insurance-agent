package com.example.carinsurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 批量任务响应（从 batch_job 映射）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobResponse {

    /** 主键 */
    private Long id;

    /** 任务编号 */
    private String jobNo;

    /** 发起业务员 id */
    private Long agentAccountId;

    /** 动作：SEND_MESSAGE / CREATE_ORDER / QUOTE */
    private String action;

    /** 总条数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 状态：PENDING / RUNNING / COMPLETED / PARTIAL_FAILED / FAILED */
    private String status;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
