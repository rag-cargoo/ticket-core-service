package com.ticketrush.api.controller;

import com.ticketrush.domain.auth.security.JwtAuthenticationFilter;
import com.ticketrush.domain.auth.service.AccessTokenDenylistService;
import com.ticketrush.domain.auth.service.AuthSessionService;
import com.ticketrush.domain.auth.service.JwtTokenProvider;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.domain.user.UserTier;
import com.ticketrush.global.config.AuthJwtProperties;
import com.ticketrush.global.config.SecurityConfig;
import com.ticketrush.global.interceptor.WaitingQueueInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, AuthJwtProperties.class})
@TestPropertySource(properties = {
        "app.auth.jwt.secret=test-auth-security-secret-key-which-is-long-enough",
        "app.auth.jwt.access-token-seconds=300",
        "app.auth.jwt.refresh-token-seconds=3600"
})
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthSessionService authSessionService;

    @MockBean
    private AccessTokenDenylistService accessTokenDenylistService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private WaitingQueueInterceptor waitingQueueInterceptor;

    @Test
    void me_shouldReturnUnauthorizedWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_shouldReturnProfileWhenAccessTokenIsValid() throws Exception {
        User user = new User("token-user", UserTier.BASIC, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 101L);
        String accessToken = jwtTokenProvider.createAccessToken(user);
        when(userRepository.findById(101L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101L))
                .andExpect(jsonPath("$.username").value("token-user"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
