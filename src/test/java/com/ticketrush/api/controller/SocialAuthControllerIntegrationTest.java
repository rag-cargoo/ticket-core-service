package com.ticketrush.api.controller;

import com.ticketrush.domain.auth.model.AuthTokenPair;
import com.ticketrush.domain.auth.model.SocialAuthorizeInfo;
import com.ticketrush.domain.auth.model.SocialLoginResult;
import com.ticketrush.domain.auth.security.JwtAuthenticationFilter;
import com.ticketrush.domain.auth.service.AccessTokenDenylistService;
import com.ticketrush.domain.auth.service.AuthSessionService;
import com.ticketrush.domain.auth.service.JwtTokenProvider;
import com.ticketrush.domain.auth.service.SocialAuthService;
import com.ticketrush.domain.user.SocialProvider;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserTier;
import com.ticketrush.global.config.AuthJwtProperties;
import com.ticketrush.global.config.SecurityConfig;
import com.ticketrush.global.interceptor.WaitingQueueInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SocialAuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, AuthJwtProperties.class})
@TestPropertySource(properties = {
        "app.auth.jwt.secret=test-social-auth-secret-key-which-is-long-enough",
        "app.auth.jwt.access-token-seconds=300",
        "app.auth.jwt.refresh-token-seconds=3600"
})
class SocialAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SocialAuthService socialAuthService;

    @MockBean
    private AuthSessionService authSessionService;

    @MockBean
    private AccessTokenDenylistService accessTokenDenylistService;

    @MockBean
    private WaitingQueueInterceptor waitingQueueInterceptor;

    @Test
    void authorizeUrl_shouldReturnProviderStateAndUrl() throws Exception {
        SocialAuthorizeInfo info = new SocialAuthorizeInfo(
                SocialProvider.KAKAO,
                "state-fixed-1",
                "https://kauth.kakao.com/oauth/authorize?client_id=fake&state=state-fixed-1"
        );
        when(socialAuthService.getAuthorizeInfo(SocialProvider.KAKAO, "state-fixed-1")).thenReturn(info);

        mockMvc.perform(get("/api/auth/social/kakao/authorize-url")
                        .param("state", "state-fixed-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("kakao"))
                .andExpect(jsonPath("$.state").value("state-fixed-1"))
                .andExpect(jsonPath("$.authorizeUrl").value(info.getAuthorizeUrl()));
    }

    @Test
    void exchangeCode_shouldReturnTokenPairAndUserProfile() throws Exception {
        User user = User.socialUser(
                "kakao_user_501",
                UserTier.BASIC,
                SocialProvider.KAKAO,
                "kakao-social-501",
                "kakao501@example.com",
                "Kakao User 501"
        );
        ReflectionTestUtils.setField(user, "id", 501L);

        SocialLoginResult loginResult = new SocialLoginResult(user, true);
        AuthTokenPair tokenPair = new AuthTokenPair("access-token-501", "refresh-token-501", 300, 3600);

        when(socialAuthService.login(SocialProvider.KAKAO, "kakao-code-501", "state-fixed-1"))
                .thenReturn(loginResult);
        when(authSessionService.issueFor(any(User.class))).thenReturn(tokenPair);

        mockMvc.perform(post("/api/auth/social/kakao/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"kakao-code-501\",\"state\":\"state-fixed-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(501L))
                .andExpect(jsonPath("$.provider").value("kakao"))
                .andExpect(jsonPath("$.socialId").value("kakao-social-501"))
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("access-token-501"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-501"))
                .andExpect(jsonPath("$.accessTokenExpiresInSeconds").value(300))
                .andExpect(jsonPath("$.refreshTokenExpiresInSeconds").value(3600));
    }

    @Test
    void authorizeUrl_shouldReturnBadRequestForUnsupportedProvider() throws Exception {
        mockMvc.perform(get("/api/auth/social/unsupported/authorize-url"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unsupported provider")));
    }
}
