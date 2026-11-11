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
 * 幂等记录（表 idempotency_key，无 version 字段）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("idempotency_key")
public class IdempotencyKey {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幂等键（如 requestId），唯一 */
    private String idemKey;

    /** 业务范围：ORDER / QUOTE / PAYMENT / OCR */
    private String scope;

    /** 请求参数指纹（SHA-256 hex） */
    private String requestHash;

    /** 状态：PROCESSING / COMPLETED / FAILED */
    private String status;

    /** 首次执行结果码 */
    private String responseCode;

    /** 首次执行结果（JSON，用于回放） */
    private String responseBody;

    /** 产生的资源类型 */
    private String resourceType;

    /** 产生的资源 ID */
    private Long resourceId;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
