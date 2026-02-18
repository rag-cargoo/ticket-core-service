package com.ticketrush.domain.auth.service;

import com.ticketrush.domain.auth.entity.RefreshToken;
import com.ticketrush.domain.auth.model.AuthTokenPair;
import com.ticketrush.domain.auth.repository.RefreshTokenRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.domain.user.UserTier;
import com.ticketrush.global.config.AuthJwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        AuthSessionServiceImpl.class,
        JwtTokenProvider.class,
        AuthJwtProperties.class,
        InMemoryAccessTokenDenylistService.class
})
@TestPropertySource(properties = {
        "app.auth.jwt.secret=test-auth-session-secret-key-which-is-long-enough",
        "app.auth.jwt.access-token-seconds=300",
        "app.auth.jwt.refresh-token-seconds=3600"
})
class AuthSessionServiceTest {

    @jakarta.annotation.Resource
    private AuthSessionService authSessionService;

    @jakarta.annotation.Resource
    private RefreshTokenRepository refreshTokenRepository;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @jakarta.annotation.Resource
    private JwtTokenProvider jwtTokenProvider;

    @jakarta.annotation.Resource
    private AccessTokenDenylistService accessTokenDenylistService;

    @Test
    void issueFor_shouldPersistRefreshTokenAndReturnTokenPair() {
        User user = userRepository.save(new User("auth-user-1", UserTier.BASIC, UserRole.USER));

        AuthTokenPair tokenPair = authSessionService.issueFor(user);

        assertThat(tokenPair.getAccessToken()).isNotBlank();
        assertThat(tokenPair.getRefreshToken()).isNotBlank();
        assertThat(refreshTokenRepository.findAll()).hasSize(1);
        RefreshToken refreshToken = refreshTokenRepository.findAll().get(0);
        assertThat(refreshToken.getUser().getId()).isEqualTo(user.getId());
        assertThat(refreshToken.isRevoked()).isFalse();
    }

    @Test
    void refresh_shouldRotateRefreshToken() {
        User user = userRepository.save(new User("auth-user-2", UserTier.BASIC, UserRole.USER));
        AuthTokenPair first = authSessionService.issueFor(user);

        AuthTokenPair second = authSessionService.refresh(first.getRefreshToken());

        assertThat(second.getAccessToken()).isNotBlank();
        assertThat(second.getRefreshToken()).isNotBlank();
        assertThat(second.getRefreshToken()).isNotEqualTo(first.getRefreshToken());
        assertThat(refreshTokenRepository.findAll()).hasSize(2);
        long revokedCount = refreshTokenRepository.findAll().stream().filter(RefreshToken::isRevoked).count();
        assertThat(revokedCount).isEqualTo(1);
    }

    @Test
    void refresh_shouldFailWhenTokenAlreadyRevoked() {
        User user = userRepository.save(new User("auth-user-3", UserTier.BASIC, UserRole.USER));
        AuthTokenPair first = authSessionService.issueFor(user);
        authSessionService.logout(first.getRefreshToken(), first.getAccessToken());

        assertThatThrownBy(() -> authSessionService.refresh(first.getRefreshToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void logout_shouldRevokeRefreshTokenAndAccessToken() {
        User user = userRepository.save(new User("auth-user-4", UserTier.BASIC, UserRole.USER));
        AuthTokenPair pair = authSessionService.issueFor(user);
        String accessTokenId = jwtTokenProvider.extractTokenId(jwtTokenProvider.parseClaims(pair.getAccessToken()));

        authSessionService.logout(pair.getRefreshToken(), pair.getAccessToken());

        RefreshToken refreshToken = refreshTokenRepository.findAll().get(0);
        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(accessTokenDenylistService.isRevoked(accessTokenId)).isTrue();
    }

    @Test
    void logout_shouldFailWhenAccessAndRefreshTokenUserMismatch() {
        User userA = userRepository.save(new User("auth-user-5a", UserTier.BASIC, UserRole.USER));
        User userB = userRepository.save(new User("auth-user-5b", UserTier.BASIC, UserRole.USER));
        AuthTokenPair pairA = authSessionService.issueFor(userA);
        AuthTokenPair pairB = authSessionService.issueFor(userB);

        assertThatThrownBy(() -> authSessionService.logout(pairA.getRefreshToken(), pairB.getAccessToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mismatch");
    }
}
