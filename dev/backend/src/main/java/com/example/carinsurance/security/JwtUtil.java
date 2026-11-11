package com.example.carinsurance.security;

import com.example.carinsurance.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具：生成/解析 Access Token 和 Refresh Token。
 *
 * <p>HS256 对称签名，密钥 ≥ 32 字节。
 * Token 类型通过 claim {@code type} 区分：access / refresh。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_USER_TYPE = "ut";
    private static final String CLAIM_AGENT_NO = "ano";
    private static final String CLAIM_TYPE = "type";

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes (got " + bytes.length + ")");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 生成 Access Token。
     */
    public String generateAccessToken(Long userId, String username, String userType, String agentNo) {
        long expireMs = jwtProperties.getAccessTokenExpireMinutes() * 60_000L;
        return build(userId, username, userType, agentNo, "access", expireMs);
    }

    /**
     * 生成 Refresh Token。
     */
    public String generateRefreshToken(Long userId, String username, String userType, String agentNo) {
        long expireMs = jwtProperties.getRefreshTokenExpireDays() * 24 * 60 * 60_000L;
        return build(userId, username, userType, agentNo, "refresh", expireMs);
    }

    private String build(Long userId, String username, String userType, String agentNo, String type, long expireMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USER_TYPE, userType);
        if (agentNo != null) claims.put(CLAIM_AGENT_NO, agentNo);
        claims.put(CLAIM_TYPE, type);

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMs))
                .signWith(signingKey())
                .compact();
    }

    /**
     * 解析 JWT 并提取 Claims，签名/过期/格式错误时返回 null。
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            log.debug("JWT 已过期: {}", ex.getMessage());
        } catch (MalformedJwtException | UnsupportedJwtException | SignatureException | IllegalArgumentException ex) {
            log.debug("JWT 无效: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * 从 Claims 构造 CurrentUser。
     */
    public CurrentUser toCurrentUser(Claims claims) {
        Long uid = claims.get(CLAIM_USER_ID, Long.class);
        String ut = claims.get(CLAIM_USER_TYPE, String.class);
        String ano = claims.get(CLAIM_AGENT_NO, String.class);
        return CurrentUser.builder()
                .id(uid)
                .username(claims.getSubject())
                .userType(ut)
                .agentNo(ano)
                .authenticated(true)
                .build();
    }

    /**
     * 获取 Access Token 剩余有效期（秒），用于响应 expiresIn 字段。
     */
    public long accessTokenExpireSeconds() {
        return jwtProperties.getAccessTokenExpireMinutes() * 60L;
    }
}
