package com.example.carinsurance.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文（从已验证 JWT 解析得到）。
 *
 * <p>Controller 层通过 {@link com.example.carinsurance.security.SecurityUtils#getCurrentUser()} 或
 * {@code @AuthenticationPrincipal} 注入，绝不相信前端传入的 userId。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser {

    /** 用户主键（sys_user.id） */
    private Long id;

    /** 登录名 */
    private String username;

    /** 角色：CUSTOMER / SALESMAN / ADMIN */
    private String userType;

    /** 业务员工号（仅 SALESMAN 有值） */
    private String agentNo;

    /** 是否已认证 */
    @Builder.Default
    private boolean authenticated = true;

    // ---------- 角色便捷判断 ----------

    public boolean isCustomer() {
        return "CUSTOMER".equals(userType);
    }

    public boolean isSalesman() {
        return "SALESMAN".equals(userType);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(userType);
    }
}
