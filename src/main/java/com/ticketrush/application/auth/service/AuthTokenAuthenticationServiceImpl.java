package com.ticketrush.application.auth.service;

import com.ticketrush.application.auth.model.AuthUserPrincipal;
import com.ticketrush.application.auth.port.inbound.AuthTokenAuthenticationUseCase;
import com.ticketrush.domain.auth.service.AccessTokenDenylistService;
import com.ticketrush.domain.user.UserRole;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthTokenAuthenticationServiceImpl implements AuthTokenAuthenticationUseCase {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenDenylistService accessTokenDenylistService;

    @Override
    public AuthUserPrincipal authenticateAccessToken(String accessToken) {
        Claims claims = jwtTokenProvider.parseClaims(accessToken);
        String tokenType = jwtTokenProvider.extractTokenType(claims);
        if (!JwtTokenProvider.TOKEN_TYPE_ACCESS.equals(tokenType)) {
            throw new IllegalArgumentException("invalid access token type");
        }

        String tokenId = jwtTokenProvider.extractTokenId(claims);
        if (accessTokenDenylistService.isRevoked(tokenId)) {
            throw new IllegalArgumentException("revoked access token");
        }

        Long userId = jwtTokenProvider.extractUserId(claims);
        String username = claims.get("username", String.class);
        UserRole role = jwtTokenProvider.extractRole(claims);
        return new AuthUserPrincipal(userId, username, role);
    }
}
