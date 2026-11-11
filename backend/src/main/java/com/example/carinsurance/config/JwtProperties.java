package com.example.carinsurance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性（从 application.yml 读取）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HS256 对称密钥（≥ 32 字节） */
    private String secret;

    /** Access Token 有效期（分钟），默认 120 = 2h */
    private long accessTokenExpireMinutes = 120;

    /** Refresh Token 有效期（天），默认 7 */
    private long refreshTokenExpireDays = 7;
}
