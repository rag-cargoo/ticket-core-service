package com.ticketrush.domain.auth.service;

import java.time.Instant;

public interface AccessTokenDenylistService {
    void revoke(String tokenId, Instant expiresAt);

    boolean isRevoked(String tokenId);
}
