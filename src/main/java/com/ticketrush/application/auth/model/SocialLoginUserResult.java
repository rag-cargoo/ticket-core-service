package com.ticketrush.application.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocialLoginUserResult {

    private final Long userId;
    private final String username;
    private final String provider;
    private final String socialId;
    private final String email;
    private final String displayName;
    private final String role;
    private final boolean newUser;
}
