package com.ticketrush.domain.auth.repository;

import com.ticketrush.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenId(String tokenId);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);
}
