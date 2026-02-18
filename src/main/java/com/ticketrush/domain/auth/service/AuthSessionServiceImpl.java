package com.ticketrush.domain.auth.service;

import com.ticketrush.domain.auth.entity.RefreshToken;
import com.ticketrush.domain.auth.model.AuthTokenPair;
import com.ticketrush.domain.auth.repository.RefreshTokenRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenDenylistService accessTokenDenylistService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuthTokenPair issueFor(User user) {
        String accessTokenId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.createAccessToken(user, accessTokenId);
        String refreshToken = jwtTokenProvider.createRefreshToken(user, refreshTokenId);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime refreshExpiresAt = now.plusSeconds(jwtTokenProvider.refreshTokenExpiresInSeconds());
        refreshTokenRepository.save(new RefreshToken(user, refreshTokenId, refreshExpiresAt, now));

        return new AuthTokenPair(
                accessToken,
                refreshToken,
                jwtTokenProvider.accessTokenExpiresInSeconds(),
                jwtTokenProvider.refreshTokenExpiresInSeconds()
        );
    }

    @Transactional
    public AuthTokenPair refresh(String refreshTokenValue) {
        Claims claims = parseRefreshClaims(refreshTokenValue);
        String tokenId = jwtTokenProvider.extractTokenId(claims);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("refresh token not found"));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (refreshToken.isRevoked() || refreshToken.isExpired(now)) {
            throw new IllegalArgumentException("refresh token expired or revoked");
        }

        Long userId = jwtTokenProvider.extractUserId(claims);
        if (!refreshToken.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("refresh token user mismatch");
        }

        refreshToken.revoke(now);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
        return issueFor(user);
    }

    @Transactional
    public void logout(String refreshTokenValue, String accessTokenValue) {
        Claims refreshClaims = parseRefreshClaims(refreshTokenValue);
        Claims accessClaims = parseAccessClaims(accessTokenValue);

        Long refreshUserId = jwtTokenProvider.extractUserId(refreshClaims);
        Long accessUserId = jwtTokenProvider.extractUserId(accessClaims);
        if (!refreshUserId.equals(accessUserId)) {
            throw new IllegalArgumentException("logout token user mismatch");
        }

        String tokenId = jwtTokenProvider.extractTokenId(refreshClaims);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("refresh token not found"));
        if (!refreshToken.getUser().getId().equals(refreshUserId)) {
            throw new IllegalArgumentException("refresh token user mismatch");
        }

        refreshToken.revoke(LocalDateTime.now(ZoneOffset.UTC));

        String accessTokenId = jwtTokenProvider.extractTokenId(accessClaims);
        accessTokenDenylistService.revoke(accessTokenId, jwtTokenProvider.extractExpiration(accessClaims));
    }

    private Claims parseRefreshClaims(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new IllegalArgumentException("refresh token is required");
        }
        Claims claims = jwtTokenProvider.parseClaims(refreshTokenValue.trim());
        if (!JwtTokenProvider.TOKEN_TYPE_REFRESH.equals(jwtTokenProvider.extractTokenType(claims))) {
            throw new IllegalArgumentException("invalid refresh token type");
        }
        return claims;
    }

    private Claims parseAccessClaims(String accessTokenValue) {
        if (accessTokenValue == null || accessTokenValue.isBlank()) {
            throw new IllegalArgumentException("access token is required");
        }
        Claims claims = jwtTokenProvider.parseClaims(accessTokenValue.trim());
        if (!JwtTokenProvider.TOKEN_TYPE_ACCESS.equals(jwtTokenProvider.extractTokenType(claims))) {
            throw new IllegalArgumentException("invalid access token type");
        }
        return claims;
    }
}
