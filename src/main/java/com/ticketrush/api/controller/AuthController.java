package com.ticketrush.api.controller;

import com.ticketrush.api.dto.auth.AuthMeResponse;
import com.ticketrush.api.dto.auth.AuthTokenResponse;
import com.ticketrush.api.dto.auth.TokenLogoutRequest;
import com.ticketrush.api.dto.auth.TokenRefreshRequest;
import com.ticketrush.domain.auth.model.AuthTokenPair;
import com.ticketrush.domain.auth.security.AuthUserPrincipal;
import com.ticketrush.domain.auth.service.AuthSessionService;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthSessionService authSessionService;
    private final UserRepository userRepository;

    @PostMapping("/token/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@RequestBody TokenRefreshRequest request) {
        AuthTokenPair tokenPair = authSessionService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(AuthTokenResponse.from(tokenPair));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody TokenLogoutRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        authSessionService.logout(request.getRefreshToken(), resolveBearerToken(authorizationHeader));
        return ResponseEntity.ok(Map.of("message", "logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me(@AuthenticationPrincipal AuthUserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("authenticated user is required");
        }
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getUserId()));
        return ResponseEntity.ok(AuthMeResponse.from(user));
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }
}
