package com.ticketrush.api.dto.auth;

import com.ticketrush.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public static AuthMeResponse from(User user) {
        return new AuthMeResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getSocialProvider() == null ? null : user.getSocialProvider().name().toLowerCase(),
                user.getSocialId(),
                user.getEmail(),
                user.getDisplayName()
        );
    }
}
