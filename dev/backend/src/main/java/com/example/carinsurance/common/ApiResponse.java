package com.example.carinsurance.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

/**
 * 统一响应包络（对齐 API 手册 1.4）
 * 成功与失败同一结构。
 *
 * 字段：
 *   code     — HTTP 状态码风格（200/400/401/403/404/409/422/429/500/502）
 *   message  — 面向用户的消息（可能本地化）
 *   data     — 业务数据，成功时返回，失败时 null
 *   errorCode — 业务语义码（机器可读，前端据此分支文案）。仅失败时出现，成功时为 null
 *   requestId — 请求链路 ID（从 MDC 取，用于串联日志）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // errorCode 为 null 时不序列化（成功响应不含）
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private String errorCode;
    private String requestId;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .data(data)
                .requestId(currentRequestId())
                .build();
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> failure(ResultCode rc) {
        return failure(rc.getCode(), rc.getMessage(), rc.name());
    }

    public static <T> ApiResponse<T> failure(ResultCode rc, String customMessage) {
        return failure(rc.getCode(), customMessage, rc.name());
    }

    public static <T> ApiResponse<T> failure(int code, String message, String errorCode) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .errorCode(errorCode)
                .requestId(currentRequestId())
                .build();
    }

    private static String currentRequestId() {
        String id = MDC.get("requestId");
        return id != null && !id.isEmpty() ? id : null;
    }
}
