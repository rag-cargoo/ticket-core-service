package com.ticketrush.application.auth.port.inbound;

import com.ticketrush.application.auth.model.AuthUserPrincipal;

public interface AuthTokenAuthenticationUseCase {

    AuthUserPrincipal authenticateAccessToken(String accessToken);
}
