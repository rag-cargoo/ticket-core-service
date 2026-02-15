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
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuthTokenPair issueFor(User user) {
        String tokenId = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user, tokenId);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime refreshExpiresAt = now.plusSeconds(jwtTokenProvider.refreshTokenExpiresInSeconds());
        refreshTokenRepository.save(new RefreshToken(user, tokenId, refreshExpiresAt, now));

        return new AuthTokenPair(
                accessToken,
                refreshToken,
                jwtTokenProvider.accessTokenExpiresInSeconds(),
                jwtTokenProvider.refreshTokenExpiresInSeconds()
        );
    }

    @Transactional
    public AuthTokenPair refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new IllegalArgumentException("refresh token is required");
        }

        Claims claims = jwtTokenProvider.parseClaims(refreshTokenValue.trim());
        if (!JwtTokenProvider.TOKEN_TYPE_REFRESH.equals(jwtTokenProvider.extractTokenType(claims))) {
            throw new IllegalArgumentException("invalid refresh token type");
        }

        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("invalid refresh token id");
        }

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
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new IllegalArgumentException("refresh token is required");
        }

        Claims claims = jwtTokenProvider.parseClaims(refreshTokenValue.trim());
        if (!JwtTokenProvider.TOKEN_TYPE_REFRESH.equals(jwtTokenProvider.extractTokenType(claims))) {
            throw new IllegalArgumentException("invalid refresh token type");
        }

        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("invalid refresh token id");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("refresh token not found"));
        refreshToken.revoke(LocalDateTime.now(ZoneOffset.UTC));
    }
}
