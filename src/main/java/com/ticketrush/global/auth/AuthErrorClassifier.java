package com.ticketrush.global.auth;

import org.springframework.util.StringUtils;

public final class AuthErrorClassifier {

    private AuthErrorClassifier() {
    }

    public static AuthErrorCode classifyUnauthorized(String detailMessage, String authorizationHeader) {
        if (StringUtils.hasText(detailMessage)) {
            return classify(detailMessage);
        }
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return AuthErrorCode.AUTH_ACCESS_TOKEN_REQUIRED;
        }
        return AuthErrorCode.AUTH_UNAUTHORIZED;
    }

    public static AuthErrorCode classify(String detailMessage) {
        if (!StringUtils.hasText(detailMessage)) {
            return AuthErrorCode.AUTH_UNAUTHORIZED;
        }

        String normalized = detailMessage.trim().toLowerCase();
        if (normalized.contains("expired jwt token")) {
            return AuthErrorCode.AUTH_TOKEN_EXPIRED;
        }
        if (normalized.contains("invalid jwt token")) {
            return AuthErrorCode.AUTH_TOKEN_INVALID;
        }
        if (normalized.contains("revoked access token")) {
            return AuthErrorCode.AUTH_ACCESS_TOKEN_REVOKED;
        }
        if (normalized.contains("access token is required")) {
            return AuthErrorCode.AUTH_ACCESS_TOKEN_REQUIRED;
        }
        if (normalized.contains("refresh token is required")) {
            return AuthErrorCode.AUTH_REFRESH_TOKEN_REQUIRED;
        }
        if (normalized.contains("refresh token not found")) {
            return AuthErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND;
        }
        if (normalized.contains("refresh token expired or revoked")) {
            return AuthErrorCode.AUTH_REFRESH_TOKEN_EXPIRED_OR_REVOKED;
        }
        if (normalized.contains("refresh token user mismatch")) {
            return AuthErrorCode.AUTH_REFRESH_TOKEN_USER_MISMATCH;
        }
        if (normalized.contains("invalid refresh token type")) {
            return AuthErrorCode.AUTH_REFRESH_TOKEN_TYPE_INVALID;
        }
        if (normalized.contains("invalid access token type")) {
            return AuthErrorCode.AUTH_ACCESS_TOKEN_TYPE_INVALID;
        }
        if (normalized.contains("logout token user mismatch")) {
            return AuthErrorCode.AUTH_LOGOUT_TOKEN_USER_MISMATCH;
        }
        if (normalized.contains("authenticated user is required")) {
            return AuthErrorCode.AUTH_AUTHENTICATED_USER_REQUIRED;
        }
        if (normalized.contains("user not found")) {
            return AuthErrorCode.AUTH_USER_NOT_FOUND;
        }
        return AuthErrorCode.AUTH_UNAUTHORIZED;
    }
}
