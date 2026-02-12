package com.ticketrush.domain.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthTokenPair {
    private final String accessToken;
    private final String refreshToken;
    private final long accessTokenExpiresInSeconds;
    private final long refreshTokenExpiresInSeconds;
}
