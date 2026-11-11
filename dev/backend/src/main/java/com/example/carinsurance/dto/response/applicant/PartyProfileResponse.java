package com.example.carinsurance.dto.response.applicant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会话人员资料响应（投保人/被保人/车主）。
 * 对应 entity ConversationParty，**敏感字段仅返回脱敏值**。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyProfileResponse {

    /** 主键 */
    private Long id;

    /** 会话 conversation.id */
    private Long conversationId;

    /** 角色：APPLICANT/INSURED/VEHICLE_OWNER */
    private String partyRole;

    /** 姓名 */
    private String name;

    /** 证件类型：ID_CARD/PASSPORT/OTHER */
    private String idType;

    /** 证件号脱敏值（**严禁返回 idNoEnc / idNoHash**） */
    private String idMasked;

    /** 手机号 */
    private String phone;

    /** 性别：MALE/FEMALE/UNKNOWN */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 证件有效期 */
    private LocalDate validTo;

    /** 与投保人关系：SELF/SPOUSE/CHILD/PARENT/OTHER */
    private String relationToApplicant;

    /** 确认状态：UNCONFIRMED/CONFIRMED/REJECTED */
    private String confirmStatus;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
