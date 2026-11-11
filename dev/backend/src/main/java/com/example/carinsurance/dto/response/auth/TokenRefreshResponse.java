package com.example.carinsurance.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 刷新响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponse {

    /** 新的 access token */
    private String accessToken;

    /** 新的刷新 token */
    private String refreshToken;

    /** access token 有效期（秒） */
    private Long expiresIn;

    /** token 类型，固定 "Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";
}
