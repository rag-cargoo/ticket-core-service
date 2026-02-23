package com.ticketrush.api.controller;

import com.ticketrush.application.auth.model.AuthUserPrincipal;
import com.ticketrush.application.auth.port.inbound.AuthTokenAuthenticationUseCase;
import com.ticketrush.application.auth.service.AuthSessionService;
import com.ticketrush.application.user.service.UserService;
import com.ticketrush.infrastructure.auth.security.JwtAuthenticationFilter;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.domain.user.UserTier;
import com.ticketrush.global.config.SecurityConfig;
import com.ticketrush.global.interceptor.WaitingQueueInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthSessionService authSessionService;

    @MockBean
    private AuthTokenAuthenticationUseCase authTokenAuthenticationUseCase;

    @MockBean
    private UserService userService;

    @MockBean
    private WaitingQueueInterceptor waitingQueueInterceptor;

    @Test
    void me_shouldReturnUnauthorizedWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTH_ACCESS_TOKEN_REQUIRED"))
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void me_shouldReturnProfileWhenAccessTokenIsValid() throws Exception {
        User user = new User("token-user", UserTier.BASIC, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 101L);
        when(userService.getUser(101L)).thenReturn(user);
        when(authTokenAuthenticationUseCase.authenticateAccessToken("valid-access-token"))
                .thenReturn(new AuthUserPrincipal(101L, "token-user", UserRole.USER));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101L))
                .andExpect(jsonPath("$.username").value("token-user"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void me_shouldReturnUnauthorizedWhenAccessTokenIsRevoked() throws Exception {
        when(authTokenAuthenticationUseCase.authenticateAccessToken("revoked-access-token"))
                .thenThrow(new IllegalArgumentException("revoked access token"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer revoked-access-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTH_ACCESS_TOKEN_REVOKED"))
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void logout_shouldReturnUnauthorizedWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"dummy\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTH_ACCESS_TOKEN_REQUIRED"))
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void refresh_shouldReturnBadRequestWithAuthErrorCodeWhenRefreshTokenMissing() throws Exception {
        when(authSessionService.refresh(null)).thenThrow(new IllegalArgumentException("refresh token is required"));

        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_TOKEN_REQUIRED"))
                .andExpect(jsonPath("$.message").value("refresh token is required"));
    }
}
