package com.ticketrush.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e, HttpServletRequest request) {
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
        log.error(">>>> [400 Error] JSON 파싱 실패! URL: {}, Cause: {}", request.getRequestURL(), e.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body("JSON Parsing Error: " + e.getMostSpecificCause().getMessage());
    }
}
