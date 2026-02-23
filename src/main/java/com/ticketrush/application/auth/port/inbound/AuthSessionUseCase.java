package com.ticketrush.application.auth.port.inbound;

import com.ticketrush.application.auth.model.AuthTokenResult;

public interface AuthSessionUseCase {

    AuthTokenResult issueForUserId(Long userId);

    AuthTokenResult refresh(String refreshTokenValue);

    void logout(String refreshTokenValue, String accessTokenValue);
}
