package com.ticketrush.api.controller;

import com.ticketrush.api.dto.auth.SocialAuthorizeUrlResponse;
import com.ticketrush.api.dto.auth.SocialLoginRequest;
import com.ticketrush.api.dto.auth.SocialLoginResponse;
import com.ticketrush.domain.auth.model.SocialAuthorizeInfo;
import com.ticketrush.domain.auth.model.SocialLoginResult;
import com.ticketrush.domain.auth.service.SocialAuthService;
import com.ticketrush.domain.user.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/social")
@RequiredArgsConstructor
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    @GetMapping("/{provider}/authorize-url")
    public ResponseEntity<SocialAuthorizeUrlResponse> getAuthorizeUrl(
            @PathVariable String provider,
            @RequestParam(required = false) String state
    ) {
        SocialProvider socialProvider = SocialProvider.from(provider);
        SocialAuthorizeInfo info = socialAuthService.getAuthorizeInfo(socialProvider, state);
        return ResponseEntity.ok(SocialAuthorizeUrlResponse.from(info));
    }

    @PostMapping("/{provider}/exchange")
    public ResponseEntity<SocialLoginResponse> exchangeCode(
            @PathVariable String provider,
            @RequestBody SocialLoginRequest request
    ) {
        SocialProvider socialProvider = SocialProvider.from(provider);
        SocialLoginResult result = socialAuthService.login(socialProvider, request.getCode(), request.getState());
        return ResponseEntity.ok(SocialLoginResponse.from(result));
    }
}
