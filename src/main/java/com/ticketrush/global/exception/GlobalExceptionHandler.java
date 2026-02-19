package com.ticketrush.global.exception;

import com.ticketrush.global.auth.AuthErrorClassifier;
import com.ticketrush.global.auth.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BAD_REQUEST = "BAD_REQUEST";
    private static final String ERROR_CONFLICT = "CONFLICT";
    private static final String ERROR_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String ERROR_REQUEST_BODY_INVALID = "REQUEST_BODY_INVALID";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e, HttpServletRequest request) {
        if (isAuthPath(request)) {
            AuthErrorCode errorCode = AuthErrorClassifier.classify(e.getMessage());
            log.warn(
                    "AUTH_MONITOR code={} status=400 method={} path={} detail={}",
                    errorCode.name(),
                    request.getMethod(),
                    request.getRequestURI(),
                    safeDetail(e.getMessage())
            );
            return buildErrorResponse(HttpStatus.BAD_REQUEST, errorCode.name(), safeDetail(e.getMessage()));
        }
        log.warn(">>>> [400 Error] URL: {}, Message: {}", request.getRequestURL(), e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_BAD_REQUEST, safeDetail(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException e, HttpServletRequest request) {
        log.warn(">>>> [409 Error] URL: {}, Message: {}", request.getRequestURL(), e.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ERROR_CONFLICT, safeDetail(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e, HttpServletRequest request) {
        log.error(">>>> [GlobalError] URL: {}, Message: {}", request.getRequestURL(), e.getMessage(), e);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ERROR_INTERNAL_SERVER_ERROR,
                safeDetail(e.getMessage())
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        String detail = "JSON Parsing Error: " + extractReadableDetail(e);
        if (isAuthPath(request)) {
            AuthErrorCode errorCode = AuthErrorCode.AUTH_REQUEST_BODY_INVALID;
            log.warn(
                    "AUTH_MONITOR code={} status=400 method={} path={} detail={}",
                    errorCode.name(),
                    request.getMethod(),
                    request.getRequestURI(),
                    detail
            );
            return buildErrorResponse(HttpStatus.BAD_REQUEST, errorCode.name(), detail);
        }
        log.error(">>>> [400 Error] JSON 파싱 실패! URL: {}, Detail: {}", request.getRequestURL(), detail);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_REQUEST_BODY_INVALID, detail);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String errorCode, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "errorCode", errorCode,
                "message", safeDetail(message)
        ));
    }

    private String extractReadableDetail(HttpMessageNotReadableException e) {
        Throwable mostSpecificCause = e.getMostSpecificCause();
        if (mostSpecificCause == null) {
            return "request body is invalid";
        }
        return safeDetail(mostSpecificCause.getMessage());
    }

    private boolean isAuthPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/api/auth") || path.startsWith("/api/reservations/v7"));
    }

    private String safeDetail(String message) {
        if (message == null || message.isBlank()) {
            return "unknown auth error";
        }
        return message;
    }
}
