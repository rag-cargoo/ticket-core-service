package com.ticketrush.api.dto.auth;

import com.ticketrush.application.auth.model.AuthTokenResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponse {
    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresInSeconds;
    private long refreshTokenExpiresInSeconds;

    public static AuthTokenResponse from(AuthTokenResult pair) {
        return new AuthTokenResponse(
                "Bearer",
                pair.getAccessToken(),
                pair.getRefreshToken(),
                pair.getAccessTokenExpiresInSeconds(),
                pair.getRefreshTokenExpiresInSeconds()
        );
    }
}
