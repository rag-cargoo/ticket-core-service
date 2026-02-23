package com.ticketrush.api.controller;

import com.ticketrush.api.dto.auth.SocialAuthorizeUrlResponse;
import com.ticketrush.api.dto.auth.SocialLoginRequest;
import com.ticketrush.api.dto.auth.SocialLoginResponse;
import com.ticketrush.application.auth.model.AuthTokenResult;
import com.ticketrush.application.auth.model.SocialAuthorizeResult;
import com.ticketrush.application.auth.model.SocialLoginUserResult;
import com.ticketrush.application.auth.port.inbound.AuthSessionUseCase;
import com.ticketrush.application.auth.port.inbound.SocialAuthUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/social")
@RequiredArgsConstructor
public class SocialAuthController {

    private final SocialAuthUseCase socialAuthUseCase;
    private final AuthSessionUseCase authSessionUseCase;

    @GetMapping("/{provider}/authorize-url")
    public ResponseEntity<SocialAuthorizeUrlResponse> getAuthorizeUrl(
            @PathVariable String provider,
            @RequestParam(required = false) String state
    ) {
        SocialAuthorizeResult info = socialAuthUseCase.getAuthorizeInfo(provider, state);
        return ResponseEntity.ok(SocialAuthorizeUrlResponse.from(info));
    }

    @PostMapping("/{provider}/exchange")
    public ResponseEntity<SocialLoginResponse> exchangeCode(
            @PathVariable String provider,
            @RequestBody SocialLoginRequest request
    ) {
        SocialLoginUserResult result = socialAuthUseCase.login(provider, request.getCode(), request.getState());
        AuthTokenResult tokenPair = authSessionUseCase.issueForUserId(result.getUserId());
        return ResponseEntity.ok(SocialLoginResponse.from(result, tokenPair));
    }
}
