package com.example.carinsurance.common;

/**
 * 统一业务状态码（对齐 API 手册 1.5）
 *
 * code      — HTTP 状态码，与 API 手册一致
 * message   — 面向用户的中文描述（后端写，前端可展示）
 * name()    — errorCode（机器可读，前端据此分支文案）
 */
public enum ResultCode {

    // 2xx
    SUCCESS(200, "成功"),

    // 4xx
    PARAM_ERROR(400, "请求参数缺失或格式错误"),
    UNAUTHORIZED(401, "未登录或登录已失效"),
    FORBIDDEN(403, "无权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    STATE_CONFLICT(409, "状态冲突，请刷新后重试"),
    VALIDATION_ERROR(422, "业务校验不通过"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // 5xx
    INTERNAL_ERROR(500, "服务内部错误，请稍后重试"),
    UPSTREAM_ERROR(502, "上游服务异常，请稍后重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
