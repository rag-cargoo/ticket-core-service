package com.ticketrush.api.dto.auth;

import com.ticketrush.domain.auth.model.AuthTokenPair;
import com.ticketrush.domain.auth.model.SocialLoginResult;
import com.ticketrush.domain.user.User;
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

    public static SocialLoginResponse from(SocialLoginResult result, AuthTokenPair tokenPair) {
        User user = result.getUser();
        String provider = user.getSocialProvider() == null ? null : user.getSocialProvider().name().toLowerCase();
        return new SocialLoginResponse(
                user.getId(),
                user.getUsername(),
                provider,
                user.getSocialId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                result.isNewUser(),
                "Bearer",
                tokenPair.getAccessToken(),
                tokenPair.getRefreshToken(),
                tokenPair.getAccessTokenExpiresInSeconds(),
                tokenPair.getRefreshTokenExpiresInSeconds()
        );
    }
}
