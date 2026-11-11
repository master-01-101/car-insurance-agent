package com.example.carinsurance.controller;

import com.example.carinsurance.common.ApiResponse;
import com.example.carinsurance.dto.request.auth.LoginRequest;
import com.example.carinsurance.dto.response.auth.LoginResponse;
import com.example.carinsurance.dto.response.auth.TokenRefreshResponse;
import com.example.carinsurance.dto.response.user.UserProfileResponse;
import com.example.carinsurance.security.SecurityUtils;
import com.example.carinsurance.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口。
 */
@Tag(name = "认证 Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "账号已禁用/锁定"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败")
    })
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "刷新 Token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刷新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "refreshToken 无效或已过期")
    })
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ApiResponse.success(authService.refresh(refreshToken));
    }

    @Operation(summary = "当前登录用户信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(authService.getUserProfile(userId));
    }
}
