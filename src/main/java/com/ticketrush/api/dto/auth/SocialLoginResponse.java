package com.ticketrush.api.dto.auth;

import com.ticketrush.application.auth.model.AuthTokenResult;
import com.ticketrush.application.auth.model.SocialLoginUserResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginResponse {
    private Long userId;
    private String username;
    private String provider;
    private String socialId;
    private String email;
    private String displayName;
    private String role;
    private boolean newUser;
    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresInSeconds;
    private long refreshTokenExpiresInSeconds;

    public static SocialLoginResponse from(SocialLoginUserResult result, AuthTokenResult tokenPair) {
        return new SocialLoginResponse(
                result.getUserId(),
                result.getUsername(),
                result.getProvider(),
                result.getSocialId(),
                result.getEmail(),
                result.getDisplayName(),
                result.getRole(),
                result.isNewUser(),
                "Bearer",
                tokenPair.getAccessToken(),
                tokenPair.getRefreshToken(),
                tokenPair.getAccessTokenExpiresInSeconds(),
                tokenPair.getRefreshTokenExpiresInSeconds()
        );
    }
}
