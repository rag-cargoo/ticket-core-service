package com.ticketrush.api.dto.auth;

import com.ticketrush.application.user.model.UserResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthMeResponse {
    private Long userId;
    private String username;
    private String role;
    private String provider;
    private String socialId;
    private String email;
    private String displayName;

    public static AuthMeResponse from(UserResult user) {
        return new AuthMeResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getSocialProvider() == null ? null : user.getSocialProvider().toLowerCase(Locale.ROOT),
                user.getSocialId(),
                user.getEmail(),
                user.getDisplayName()
        );
    }
}
