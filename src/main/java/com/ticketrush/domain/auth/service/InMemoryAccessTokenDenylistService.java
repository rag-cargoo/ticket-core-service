package com.ticketrush.domain.auth.service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccessTokenDenylistService implements AccessTokenDenylistService {

    private final Map<String, Instant> revokedUntilByTokenId = new ConcurrentHashMap<>();

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        Instant normalizedExpiresAt = expiresAt == null ? Instant.now().plusSeconds(1) : expiresAt;
        revokedUntilByTokenId.put(tokenId, normalizedExpiresAt);
        cleanupExpired(Instant.now());
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        Instant now = Instant.now();
        Instant revokedUntil = revokedUntilByTokenId.get(tokenId);
        if (revokedUntil == null) {
            return false;
        }
        if (!revokedUntil.isAfter(now)) {
            revokedUntilByTokenId.remove(tokenId);
            return false;
        }
        return true;
    }

    private void cleanupExpired(Instant now) {
        Iterator<Map.Entry<String, Instant>> iterator = revokedUntilByTokenId.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Instant> entry = iterator.next();
            if (!entry.getValue().isAfter(now)) {
                iterator.remove();
            }
        }
    }
}
