package com.ticketrush.domain.auth.security;

import com.ticketrush.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthUserPrincipal {
    private Long userId;
    private String username;
    private UserRole role;
}
