package com.ticketrush.domain.auth.service;

import com.ticketrush.global.config.AuthJwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RedisAccessTokenDenylistService implements AccessTokenDenylistService {

    private static final Duration MIN_TTL = Duration.ofSeconds(1);

    private final StringRedisTemplate redisTemplate;
    private final AuthJwtProperties authJwtProperties;
    private final Map<String, Instant> fallbackRevokedUntilByTokenId = new ConcurrentHashMap<>();

    public RedisAccessTokenDenylistService(
            StringRedisTemplate redisTemplate,
            AuthJwtProperties authJwtProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.authJwtProperties = authJwtProperties;
    }

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        if (!StringUtils.hasText(tokenId)) {
            return;
        }

        Instant normalizedExpiresAt = expiresAt == null ? Instant.now().plusSeconds(1) : expiresAt;
        Duration ttl = Duration.between(Instant.now(), normalizedExpiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            ttl = MIN_TTL;
        }

        fallbackRevokedUntilByTokenId.put(tokenId, normalizedExpiresAt);
        cleanupFallback(Instant.now());

        try {
            redisTemplate.opsForValue().set(key(tokenId), "1", ttl);
        } catch (RuntimeException ex) {
            log.warn(">>>> [Auth] access token denylist redis write failed, using fallback map. tokenId={}", tokenId, ex);
        }
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (!StringUtils.hasText(tokenId)) {
            return false;
        }

        Instant now = Instant.now();
        if (isFallbackRevoked(tokenId, now)) {
            return true;
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(tokenId)));
        } catch (RuntimeException ex) {
            log.warn(">>>> [Auth] access token denylist redis read failed, using fallback map. tokenId={}", tokenId, ex);
            return isFallbackRevoked(tokenId, now);
        }
    }

    private boolean isFallbackRevoked(String tokenId, Instant now) {
        Instant revokedUntil = fallbackRevokedUntilByTokenId.get(tokenId);
        if (revokedUntil == null) {
            return false;
        }
        if (!revokedUntil.isAfter(now)) {
            fallbackRevokedUntilByTokenId.remove(tokenId);
            return false;
        }
        return true;
    }

    private void cleanupFallback(Instant now) {
        Iterator<Map.Entry<String, Instant>> iterator = fallbackRevokedUntilByTokenId.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Instant> entry = iterator.next();
            if (!entry.getValue().isAfter(now)) {
                iterator.remove();
            }
        }
    }

    private String key(String tokenId) {
        return authJwtProperties.getAccessTokenBlocklistKeyPrefix() + tokenId;
    }
}
