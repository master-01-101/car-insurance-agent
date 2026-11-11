package com.example.carinsurance.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户资料响应（不含敏感字段）。
 * 对应 entity SysUser，**严禁暴露 passwordHash**。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    /** 主键 */
    private Long id;

    /** 登录名 */
    private String username;

    /** 用户角色：CUSTOMER/SALESMAN/ADMIN */
    private String userType;

    /** 手机号 */
    private String phone;

    /** 业务员工号 */
    private String agentNo;

    /** 所属机构/团队 */
    private String orgName;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 头像地址 */
    private String avatarUrl;

    /** 状态：ACTIVE/DISABLED/LOCKED */
    private String status;

    /** 最近登录时间 */
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
