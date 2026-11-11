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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会话人员资料（投保人/被保人/车主）。
 * 对应表 conversation_party。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("conversation_party")
public class ConversationParty {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 角色：APPLICANT/INSURED/VEHICLE_OWNER */
    private String partyRole;

    /** 姓名 */
    private String name;

    /** 证件类型：ID_CARD/PASSPORT/OTHER */
    private String idType;

    /** 证件号密文 AES-256-GCM（敏感加密字段） */
    private byte[] idNoEnc;

    /** 证件号 HMAC-SHA256 哈希，用于查重（64位十六进制） */
    private String idNoHash;

    /** 证件号脱敏值 */
    private String idMasked;

    /** 证件有效期 */
    private LocalDate validTo;

    /** 手机号 */
    private String phone;

    /** 性别：MALE/FEMALE/UNKNOWN */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 与投保人关系：SELF/SPOUSE/CHILD/PARENT/OTHER */
    private String relationToApplicant;

    /** 依据的 ocr_record.id */
    private Long sourceOcrId;

    /** 资料版本 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 确认状态：UNCONFIRMED/CONFIRMED/REJECTED */
    private String confirmStatus;

    /** 确认时间 */
    private LocalDateTime confirmedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
