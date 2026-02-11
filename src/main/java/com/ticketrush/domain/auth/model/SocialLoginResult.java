package com.ticketrush.domain.auth.model;

import com.ticketrush.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocialLoginResult {
    private final User user;
    private final boolean newUser;
}
