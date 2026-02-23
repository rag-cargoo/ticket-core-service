package com.ticketrush.application.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthTokenResult {

    private final String accessToken;
    private final String refreshToken;
    private final long accessTokenExpiresInSeconds;
    private final long refreshTokenExpiresInSeconds;
}
