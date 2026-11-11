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

import java.time.LocalDateTime;

/**
 * 批量任务（表 batch_job，无 version 字段）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("batch_job")
public class BatchJob {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号，唯一 */
    private String jobNo;

    /** 发起业务员 sys_user.id */
    private Long agentAccountId;

    /** 前端幂等请求 ID，唯一 */
    private String clientRequestId;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
