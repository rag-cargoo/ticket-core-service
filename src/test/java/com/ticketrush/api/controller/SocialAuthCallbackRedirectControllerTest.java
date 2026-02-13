package com.ticketrush.api.controller;

import com.ticketrush.domain.auth.security.JwtAuthenticationFilter;
import com.ticketrush.global.interceptor.WaitingQueueInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SocialAuthCallbackRedirectController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class SocialAuthCallbackRedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WaitingQueueInterceptor waitingQueueInterceptor;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void callbackRoute_shouldRedirectToU1CallbackWithOriginalQuery() throws Exception {
        mockMvc.perform(get("/login/oauth2/code/naver")
                        .param("code", "sample-code")
                        .param("state", "u1_naver_1770963678337_d4xmuaov06"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/ux/u1/callback.html?*"))
                .andExpect(header().string("Location", containsString("code=sample-code")))
                .andExpect(header().string("Location", containsString("state=u1_naver_1770963678337_d4xmuaov06")))
                .andExpect(header().string("Location", containsString("provider=naver")));
    }
}
