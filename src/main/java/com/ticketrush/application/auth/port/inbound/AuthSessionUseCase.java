package com.ticketrush.application.auth.port.inbound;

import com.ticketrush.domain.auth.model.AuthTokenPair;
import com.ticketrush.domain.user.User;

public interface AuthSessionUseCase {

    AuthTokenPair issueFor(User user);

    AuthTokenPair refresh(String refreshTokenValue);

    void logout(String refreshTokenValue, String accessTokenValue);
}
