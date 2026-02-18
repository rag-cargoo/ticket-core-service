package com.ticketrush.domain.auth.service;

import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.global.config.AuthJwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final AuthJwtProperties authJwtProperties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = authJwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.auth.jwt.secret is required");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(normalizeKeyLength(keyBytes));
    }

    public String createAccessToken(User user) {
        return createAccessToken(user, UUID.randomUUID().toString());
    }

    public String createAccessToken(User user, String tokenId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(authJwtProperties.getAccessTokenSeconds(), ChronoUnit.SECONDS);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .issuer(authJwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("typ", TOKEN_TYPE_ACCESS)
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .signWith(signingKey)
                .compact();
    }

    public String createRefreshToken(User user, String tokenId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(authJwtProperties.getRefreshTokenSeconds(), ChronoUnit.SECONDS);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .issuer(authJwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("typ", TOKEN_TYPE_REFRESH)
                .claim("role", user.getRole().name())
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("expired jwt token");
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid jwt token");
        }
    }

    public long accessTokenExpiresInSeconds() {
        return authJwtProperties.getAccessTokenSeconds();
    }

    public long refreshTokenExpiresInSeconds() {
        return authJwtProperties.getRefreshTokenSeconds();
    }

    public Long extractUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public UserRole extractRole(Claims claims) {
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("invalid jwt token role");
        }
        return UserRole.valueOf(role);
    }

    public String extractTokenType(Claims claims) {
        String type = claims.get("typ", String.class);
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("invalid jwt token type");
        }
        return type;
    }

    public String extractTokenId(Claims claims) {
        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("invalid jwt token id");
        }
        return tokenId;
    }

    public Instant extractExpiration(Claims claims) {
        Date expiresAt = claims.getExpiration();
        if (expiresAt == null) {
            throw new IllegalArgumentException("invalid jwt token expiration");
        }
        return expiresAt.toInstant();
    }

    private byte[] normalizeKeyLength(byte[] keyBytes) {
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        byte[] expanded = new byte[32];
        for (int i = 0; i < expanded.length; i++) {
            expanded[i] = keyBytes[i % keyBytes.length];
        }
        return expanded;
    }
}
