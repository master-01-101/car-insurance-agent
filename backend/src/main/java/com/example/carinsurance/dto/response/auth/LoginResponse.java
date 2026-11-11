package com.example.carinsurance.dto.response.auth;

import com.example.carinsurance.dto.response.user.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT access token */
    private String accessToken;

    /** 刷新 token */
    private String refreshToken;

    /** access token 有效期（秒） */
    private Long expiresIn;

    /** token 类型，固定 "Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";

    /** 当前用户信息 */
    private UserProfileResponse user;
}
