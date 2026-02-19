package com.ticketrush.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadRequest_shouldReturnJsonEnvelopeForNonAuthPath() {
        HttpServletRequest request = request("/api/users/1", "GET");

        ResponseEntity<Map<String, Object>> response =
                handler.handleBadRequest(new IllegalArgumentException("amount must be positive"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("errorCode", "BAD_REQUEST");
        assertThat(response.getBody()).containsEntry("message", "amount must be positive");
    }

    @Test
    void handleBadRequest_shouldUseAuthErrorCodeOnAuthPath() {
        HttpServletRequest request = request("/api/auth/refresh", "POST");

        ResponseEntity<Map<String, Object>> response =
                handler.handleBadRequest(new IllegalArgumentException("refresh token is required"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("errorCode", "AUTH_REFRESH_TOKEN_REQUIRED");
        assertThat(response.getBody()).containsEntry("message", "refresh token is required");
    }

    @Test
    void handleConflict_shouldReturnJsonEnvelopeForNonAuthPath() {
        HttpServletRequest request = request("/api/reservations/v6/1/confirm", "POST");

        ResponseEntity<Map<String, Object>> response =
                handler.handleConflict(new IllegalStateException("Insufficient wallet balance."), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("errorCode", "CONFLICT");
        assertThat(response.getBody()).containsEntry("message", "Insufficient wallet balance.");
    }

    @Test
    void handleReadable_shouldReturnJsonEnvelopeForNonAuthPath() {
        HttpServletRequest request = request("/api/users/1/wallet/charges", "POST");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "invalid json body",
                new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<Map<String, Object>> response = handler.handleReadable(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("errorCode", "REQUEST_BODY_INVALID");
        assertThat(response.getBody().get("message").toString()).startsWith("JSON Parsing Error:");
    }

    private HttpServletRequest request(String path, String method) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080" + path));
        when(request.getMethod()).thenReturn(method);
        return request;
    }
}
