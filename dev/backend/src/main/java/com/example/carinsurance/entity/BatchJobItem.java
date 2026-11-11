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
 * 批量任务明细（表 batch_job_item，无 version 字段）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("batch_job_item")
public class BatchJobItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批量任务 batch_job.id */
    private Long batchId;

    /** 客户 sys_user.id */
    private Long customerId;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 车牌号 */
    private String plateNumber;

    /** 单项请求 ID */
    private String requestId;

    /** 状态：PENDING / RUNNING / SUCCEEDED / FAILED */
    private String status;

    /** 错误码 */
    private String errorCode;

    /** 失败原因 */
    private String errorMessage;

    /** AI 回复消息 chat_message.id */
    private Long replyMessageId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
