package com.example.carinsurance.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /** 登录名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度 3-50 字符")
    private String username;

    /** 密码（明文） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度 8-100 字符")
    @Pattern(regexp = ".*[A-Za-z].*\\d.*", message = "密码需同时包含字母和数字")
    private String password;

    /** 用户角色：CUSTOMER/SALESMAN/ADMIN */
    @NotBlank(message = "用户类型不能为空")
    @Pattern(regexp = "CUSTOMER|SALESMAN|ADMIN", message = "用户类型必须是 CUSTOMER/SALESMAN/ADMIN")
    private String userType;

    /** 手机号（可选） */
    @Size(max = 20, message = "手机号最多 20 字符")
    private String phone;

    /** 真实姓名（可选） */
    @Size(max = 50, message = "真实姓名最多 50 字符")
    private String realName;

    /** 业务员工号，仅 SALESMAN 需要，可空 */
    @Size(max = 32, message = "业务员工号最多 32 字符")
    private String agentNo;

    /** 所属机构/团队（可选） */
    @Size(max = 100, message = "机构名称最多 100 字符")
    private String orgName;
}
