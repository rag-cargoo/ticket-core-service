package com.ticketrush.domain.auth.service;

import com.ticketrush.domain.auth.model.AuthTokenPair;
import com.ticketrush.domain.user.User;

public interface AuthSessionService {
    AuthTokenPair issueFor(User user);

    AuthTokenPair refresh(String refreshTokenValue);

    void logout(String refreshTokenValue, String accessTokenValue);
}
