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
 * 审计日志（表 audit_log，无 version / 无 updated_at）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_log")
public class AuditLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 链路追踪 ID */
    private String traceId;

    /** 操作者 sys_user.id */
    private Long operatorId;

    /** 操作者类型：CUSTOMER / SALESMAN / ADMIN / SYSTEM / AI */
    private String operatorType;

    /** 动作：LOGIN / ORDER_CREATE / PAY_CALLBACK / ... */
    private String action;

    /** 资源类型 */
    private String resourceType;

    /** 资源 ID */
    private Long resourceId;

    /** 结果：SUCCESS / FAILED */
    private String result;

    /** 来源 IP */
    private String ip;

    /** 详情（必须脱敏后的 JSON） */
    private String detailJson;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
