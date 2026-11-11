package com.example.carinsurance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.carinsurance.common.ResultCode;
import com.example.carinsurance.dto.request.auth.LoginRequest;
import com.example.carinsurance.dto.response.auth.LoginResponse;
import com.example.carinsurance.dto.response.auth.TokenRefreshResponse;
import com.example.carinsurance.dto.response.user.UserProfileResponse;
import com.example.carinsurance.entity.SysUser;
import com.example.carinsurance.exception.BusinessException;
import com.example.carinsurance.mapper.SysUserMapper;
import com.example.carinsurance.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证业务：登录、token 刷新、当前用户信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录。
     */
    @Transactional
    public LoginResponse login(LoginRequest req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername())
                .last("LIMIT 1"));

        if (user == null) {
            log.warn("登录失败：用户不存在 username={}", req.getUsername());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            log.warn("登录失败：密码错误 username={}", req.getUsername());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if ("DISABLED".equals(user.getStatus()) || "LOCKED".equals(user.getStatus())) {
            log.warn("登录失败：账号已禁用/锁定 username={} status={}", req.getUsername(), user.getStatus());
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已禁用或锁定");
        }

        // 生成 token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getUserType(), user.getAgentNo());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getUserType(), user.getAgentNo());

        // 更新最近登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功 userId={} username={} type={}", user.getId(), user.getUsername(), user.getUserType());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.accessTokenExpireSeconds())
                .tokenType("Bearer")
                .user(toProfile(user))
                .build();
    }

    /**
     * 用 refreshToken 换取新的 accessToken + refreshToken。
     */
    public TokenRefreshResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refreshToken 不能为空");
        }
        Claims claims = jwtUtil.parseClaims(refreshToken);
        if (claims == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refreshToken 无效或已过期");
        }
        if (!"refresh".equals(claims.get("type"))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的 token 类型");
        }

        Long uid = claims.get("uid", Long.class);
        String ut = claims.get("ut", String.class);
        String ano = claims.get("ano", String.class);
        String username = claims.getSubject();

        String newAccess = jwtUtil.generateAccessToken(uid, username, ut, ano);
        String newRefresh = jwtUtil.generateRefreshToken(uid, username, ut, ano);

        return TokenRefreshResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .expiresIn(jwtUtil.accessTokenExpireSeconds())
                .tokenType("Bearer")
                .build();
    }

    /**
     * 获取当前用户资料（仅安全字段）。
     */
    public UserProfileResponse getUserProfile(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        return toProfile(user);
    }

    // ---------- Entity → DTO ----------

    private UserProfileResponse toProfile(SysUser u) {
        return UserProfileResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .userType(u.getUserType())
                .phone(u.getPhone())
                .agentNo(u.getAgentNo())
                .orgName(u.getOrgName())
                .realName(u.getRealName())
                .email(u.getEmail())
                .avatarUrl(u.getAvatarUrl())
                .status(u.getStatus())
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
