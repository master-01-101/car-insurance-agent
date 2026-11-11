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
 * 资料草稿表（待用户确认）。
 * 对应表 profile_draft。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("profile_draft")
public class ProfileDraft {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 草稿编号，唯一 */
    private String draftNo;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 类型：VEHICLE_PROFILE/PARTY_PROFILE */
    private String kind;

    /** 人员角色，车辆草稿为空 */
    private String partyRole;

    /** 目标 ID（vehicle 或 conversation_party） */
    private Long targetId;

    /** 草稿版本 */
    private Integer draftVersion;

    /** 依据的 OCR 记录 ID 列表（JSON） */
    private String ocrRecordIds;

    /** 脱敏后的展示字段（JSON） */
    private String displayFields;

    /** 缺失字段（JSON） */
    private String missingFields;

    /** 异常字段（JSON） */
    private String abnormalFields;

    /** 是否可确认：1-是 0-否 */
    private Integer canConfirm;

    /** 状态：PENDING/CONFIRMED/EXPIRED/CANCELLED */
    private String status;

    /** 创建人/服务 sys_user.id */
    private Long createdBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
