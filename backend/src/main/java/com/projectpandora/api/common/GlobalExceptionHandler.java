package com.projectpandora.api.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiError.of(ex.getStatus(), HttpStatus.valueOf(ex.getStatus()).getReasonPhrase(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(FieldError::getDefaultMessage)
                        .orElse("请求参数不合法");
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Bad Request", message, request.getRequestURI()));
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
                       org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformed(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "Bad Request",
                "请求格式不正确，请检查日期和 JSON", request.getRequestURI()));
    }

    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,
                       org.springframework.dao.PessimisticLockingFailureException.class,
                       org.springframework.dao.DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> handleConflict(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(409).body(ApiError.of(409, "Conflict", "数据已变化或发生冲突，请刷新后重试", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(500)
                .body(ApiError.of(500, "Internal Server Error", "服务器内部错误", request.getRequestURI()));
    }
}
