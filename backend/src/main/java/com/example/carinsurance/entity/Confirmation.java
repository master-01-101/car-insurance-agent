package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户确认记录表（绑定目标版本）。
 * 对应表 confirmation。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("confirmation")
public class Confirmation {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 确认类型：PLAN/VEHICLE_PROFILE/PARTY_PROFILE */
    private String kind;

    /** 确认目标 ID */
    private Long targetId;

    /** 确认目标版本 */
    private Integer targetVersion;

    /** 状态：PENDING/CONFIRMED/EXPIRED/SUPERSEDED/CANCELLED */
    private String status;

    /** 展示数据（脱敏，JSON） */
    private String displayData;

    /** 展示内容摘要，防篡改（SHA256，64位十六进制） */
    private String displayDigest;

    /** 缺失字段（JSON） */
    private String missingFields;

    /** 异常字段（JSON） */
    private String abnormalFields;

    /** 确认令牌哈希，仅存摘要（SHA256，64位十六进制） */
    private String tokenHash;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 确认人 sys_user.id */
    private Long confirmedBy;

    /** 确认时间 */
    private LocalDateTime confirmedAt;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
