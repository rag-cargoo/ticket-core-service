package com.ticketrush.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
