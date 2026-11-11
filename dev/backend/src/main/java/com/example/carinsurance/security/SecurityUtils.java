package com.example.carinsurance.security;

import com.example.carinsurance.exception.BusinessException;
import com.example.carinsurance.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：从 SecurityContext 获取当前用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 获取当前登录用户，未登录则抛出 401。
     */
    public static CurrentUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CurrentUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已失效");
        }
        return (CurrentUser) auth.getPrincipal();
    }

    /**
     * 获取当前用户 id，未登录则抛异常。
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
