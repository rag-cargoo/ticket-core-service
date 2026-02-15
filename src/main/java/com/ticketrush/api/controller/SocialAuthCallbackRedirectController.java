package com.ticketrush.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;

@Controller
public class SocialAuthCallbackRedirectController {

    private static final String DEFAULT_U1_CALLBACK_PATH = "/ux/u1/callback.html";
    private final String u1CallbackUrl;

    public SocialAuthCallbackRedirectController(
            @Value("${app.frontend.u1-callback-url:/ux/u1/callback.html}") String u1CallbackUrl
    ) {
        this.u1CallbackUrl = u1CallbackUrl;
    }

    @GetMapping("/login/oauth2/code/{provider}")
    public String redirectToU1Callback(@PathVariable String provider,
                                       @RequestParam MultiValueMap<String, String> queryParams) {
        MultiValueMap<String, String> merged = new LinkedMultiValueMap<>(queryParams);
        String normalizedProvider = provider == null ? "" : provider.toLowerCase(Locale.ROOT);

        if (!merged.containsKey("provider") && !normalizedProvider.isBlank()) {
            merged.add("provider", normalizedProvider);
        }

        String redirectTarget = UriComponentsBuilder.fromUriString(resolveU1CallbackUrl())
                .queryParams(merged)
                .build()
                .encode()
                .toUriString();
        return "redirect:" + redirectTarget;
    }

    private String resolveU1CallbackUrl() {
        if (!StringUtils.hasText(u1CallbackUrl)) {
            return DEFAULT_U1_CALLBACK_PATH;
        }

        String normalized = u1CallbackUrl.trim();
        if (normalized.startsWith("/") || normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }

        throw new IllegalArgumentException("app.frontend.u1-callback-url must start with '/' or 'http(s)://'");
    }
}
