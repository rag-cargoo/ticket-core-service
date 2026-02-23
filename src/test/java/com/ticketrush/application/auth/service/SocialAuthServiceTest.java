package com.ticketrush.application.auth.service;

import com.ticketrush.application.auth.model.SocialAuthorizeResult;
import com.ticketrush.application.auth.model.SocialLoginUserResult;
import com.ticketrush.domain.auth.model.SocialProfile;
import com.ticketrush.domain.auth.oauth.SocialOAuthClient;
import com.ticketrush.domain.user.SocialProvider;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({SocialAuthServiceImpl.class, SocialAuthServiceTest.TestConfig.class})
class SocialAuthServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        SocialOAuthClient kakaoFakeClient() {
            return new FakeOAuthClient(SocialProvider.KAKAO);
        }

        @Bean
        SocialOAuthClient naverFakeClient() {
            return new FakeOAuthClient(SocialProvider.NAVER);
        }
    }

    @jakarta.annotation.Resource
    private SocialAuthService socialAuthService;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @Test
    void login_shouldCreateSocialUserOnFirstLogin() {
        SocialLoginUserResult result = socialAuthService.login("kakao", "first-code", null);

        assertThat(result.isNewUser()).isTrue();
        User user = userRepository.findById(result.getUserId()).orElseThrow();
        assertThat(user.getSocialProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(user.getSocialId()).isEqualTo("kakao-fixed-id");
        assertThat(user.getEmail()).isEqualTo("first-code@kakao.example.com");
        assertThat(userRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "kakao-fixed-id"))
                .isPresent();
    }

    @Test
    void login_shouldReuseExistingUserAndUpdateProfile() {
        SocialLoginUserResult first = socialAuthService.login("kakao", "before", null);
        SocialLoginUserResult second = socialAuthService.login("kakao", "after", null);

        assertThat(first.getUserId()).isEqualTo(second.getUserId());
        assertThat(second.isNewUser()).isFalse();
        assertThat(second.getEmail()).isEqualTo("after@kakao.example.com");
        assertThat(second.getDisplayName()).isEqualTo("KAKAO-after");
    }

    @Test
    void authorizeUrl_shouldGenerateStateWhenNotProvided() {
        SocialAuthorizeResult info = socialAuthService.getAuthorizeInfo("naver", null);

        assertThat(info.getState()).isNotBlank();
        assertThat(info.getAuthorizeUrl()).contains("state=" + info.getState());
    }

    @Test
    void naverLogin_shouldRequireState() {
        assertThatThrownBy(() -> socialAuthService.login("naver", "code", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("state is required");
    }

    static class FakeOAuthClient implements SocialOAuthClient {
        private final SocialProvider provider;

        FakeOAuthClient(SocialProvider provider) {
            this.provider = provider;
        }

        @Override
        public SocialProvider provider() {
            return provider;
        }

        @Override
        public String buildAuthorizeUrl(String state) {
            if (provider == SocialProvider.NAVER && !StringUtils.hasText(state)) {
                throw new IllegalArgumentException("state is required for naver authorize url");
            }
            return "https://example.com/oauth/" + provider.name().toLowerCase() + "?state=" + state;
        }

        @Override
        public SocialProfile fetchProfile(String code, String state) {
            if (provider == SocialProvider.NAVER && !StringUtils.hasText(state)) {
                throw new IllegalArgumentException("state is required for naver token exchange");
            }
            String lowerProvider = provider.name().toLowerCase();
            String socialId = lowerProvider + "-fixed-id";
            String email = code + "@" + lowerProvider + ".example.com";
            String displayName = provider.name() + "-" + code;
            return new SocialProfile(provider, socialId, email, displayName);
        }
    }
}
