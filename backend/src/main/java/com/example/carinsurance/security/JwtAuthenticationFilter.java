package com.example.carinsurance.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：从 Authorization: Bearer xxx 头提取 token → 验证 → 构造 SecurityContext。
 *
 * <p>不抛出异常，token 无效时静默放行（由后续的 Authorization 检查处理）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            Claims claims = jwtUtil.parseClaims(token);
            if (claims != null && "access".equals(claims.get("type"))) {
                CurrentUser currentUser = jwtUtil.toCurrentUser(claims);

                // 角色映射：ROLE_CUSTOMER / ROLE_SALESMAN / ROLE_ADMIN
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + currentUser.getUserType())
                );

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        currentUser, null, authorities
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
