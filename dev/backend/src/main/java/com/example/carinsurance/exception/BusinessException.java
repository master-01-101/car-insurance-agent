package com.example.carinsurance.exception;

import com.example.carinsurance.common.ResultCode;

/**
 * 业务异常：后端主动抛出的、预期可控的异常（参数、权限、状态冲突等）
 * 构造时可指定自定义 errorCode（机器可读的业务语义码）
 */
public class BusinessException extends RuntimeException {

    private final int code;
    /** 业务语义码（对齐 API 手册 1.5 的 errorCode，如 PLAN_NOT_CONFIRMED） */
    private final String errorCode;

    public BusinessException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
        this.errorCode = rc.name();
    }

    public BusinessException(ResultCode rc, String customMessage) {
        super(customMessage);
        this.code = rc.getCode();
        this.errorCode = rc.name();
    }

    public BusinessException(int code, String message, String errorCode) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

    /** 仅自定义 message，errorCode 跟 ResultCode 走 */
    public BusinessException(ResultCode rc, String customMessage, String customErrorCode) {
        super(customMessage);
        this.code = rc.getCode();
        this.errorCode = customErrorCode;
    }

    public int getCode() {
        return code;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
