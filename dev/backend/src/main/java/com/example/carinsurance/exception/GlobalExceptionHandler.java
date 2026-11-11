package com.example.carinsurance.exception;

import com.example.carinsurance.common.ApiResponse;
import com.example.carinsurance.common.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 原则：
 *  - 返回统一的 ApiResponse 结构
 *  - HTTP 状态码与业务 code 保持一致
 *  - 不泄露 SQL、密码、堆栈
 *  - 日志里打印完整堆栈（服务端排障），响应里只给中文 message
 *
 * 处理顺序（从上到下，Spring 按子类优先匹配）：
 *   1. 业务异常（后端主动抛出）
 *   2. 请求体 JSON 格式错误
 *   3. 参数校验异常（@Valid / @Validated 触发）
 *   4. 其他参数错误
 *   5. 资源不存在
 *   6. 数据库异常（唯一键冲突 / 通用）
 *   7. 未知兜底
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 1. 业务异常（后端主动抛出）----------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("业务异常: code={}, errorCode={}, message={}", ex.getCode(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getCode())
                .body(ApiResponse.failure(ex.getCode(), ex.getMessage(), ex.getErrorCode()));
    }

    // ---------- 2. 请求体 JSON 格式错误（反序列化失败）----------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体格式错误: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ResultCode.PARAM_ERROR.getCode(), "请求体 JSON 格式错误，请检查字段类型和值", "INVALID_JSON"));
    }

    // ---------- 3. 参数校验异常（返回 422，对齐 API 手册）----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("请求体参数校验失败: {}", message);
        return ResponseEntity.unprocessableEntity()
                .body(ApiResponse.failure(ResultCode.VALIDATION_ERROR.getCode(), message, "VALIDATION_FAILED"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.unprocessableEntity()
                .body(ApiResponse.failure(ResultCode.VALIDATION_ERROR.getCode(), message, "VALIDATION_FAILED"));
    }

    // ---------- 4. 其他参数错误 ----------

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "请求参数错误";
        log.warn("请求参数错误: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ResultCode.PARAM_ERROR.getCode(), message, "INVALID_ARGUMENT"));
    }

    // ---------- 5. 资源不存在 ----------

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        log.warn("资源不存在: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage(), "NOT_FOUND"));
    }

    // ---------- 6. 数据库异常 ----------

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("唯一键冲突", ex); // 日志里打完整异常（含 SQL 上下文）
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ResultCode.STATE_CONFLICT.getCode(), "数据已存在，请勿重复提交", "DUPLICATE_ENTRY"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        // 可能是外键约束、字段校验等
        log.warn("数据库异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getMessage(), "DB_ERROR"));
    }

    // ---------- 7. HTTP 方法 / Content-Type 不支持 ----------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP 方法不支持: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.failure(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不被允许", "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Content-Type 不支持: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.failure(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "不支持的请求 Content-Type", "UNSUPPORTED_MEDIA_TYPE"));
    }

    // ---------- 7b. 文件上传超限 ----------

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("文件上传超限: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE) // 413
                .body(ApiResponse.failure(413, "文件过大，请小于 10MB", "FILE_TOO_LARGE"));
    }

    // ---------- 8. 未知兜底 ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("未处理异常（堆栈仅服务端可见）", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ResultCode.INTERNAL_ERROR));
    }
}
