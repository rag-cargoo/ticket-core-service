package com.ticketrush.global.exception;

import com.ticketrush.global.auth.AuthErrorClassifier;
import com.ticketrush.global.auth.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e, HttpServletRequest request) {
        if (isAuthPath(request)) {
            AuthErrorCode errorCode = AuthErrorClassifier.classify(e.getMessage());
            log.warn(
                    "AUTH_MONITOR code={} status=400 method={} path={} detail={}",
                    errorCode.name(),
                    request.getMethod(),
                    request.getRequestURI(),
                    safeDetail(e.getMessage())
            );
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "errorCode", errorCode.name(),
                    "message", safeDetail(e.getMessage())
            ));
        }
        log.warn(">>>> [400 Error] URL: {}, Message: {}", request.getRequestURL(), e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleConflict(IllegalStateException e, HttpServletRequest request) {
        log.warn(">>>> [409 Error] URL: {}, Message: {}", request.getRequestURL(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception e, HttpServletRequest request) {
        log.error(">>>> [GlobalError] URL: {}, Message: {}", request.getRequestURL(), e.getMessage(), e);
        return ResponseEntity.internalServerError().body(e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        if (isAuthPath(request)) {
            String detail = "JSON Parsing Error: " + safeDetail(e.getMostSpecificCause().getMessage());
            AuthErrorCode errorCode = AuthErrorCode.AUTH_REQUEST_BODY_INVALID;
            log.warn(
                    "AUTH_MONITOR code={} status=400 method={} path={} detail={}",
                    errorCode.name(),
                    request.getMethod(),
                    request.getRequestURI(),
                    detail
            );
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "errorCode", errorCode.name(),
                    "message", detail
            ));
        }
        log.error(">>>> [400 Error] JSON 파싱 실패! URL: {}, Cause: {}", request.getRequestURL(), e.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body("JSON Parsing Error: " + e.getMostSpecificCause().getMessage());
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
