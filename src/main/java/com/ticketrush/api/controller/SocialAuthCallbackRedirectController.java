package com.ticketrush.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;

@Controller
public class SocialAuthCallbackRedirectController {

    private static final String U1_CALLBACK_PATH = "/ux/u1/callback.html";

    @GetMapping("/login/oauth2/code/{provider}")
    public String redirectToU1Callback(@PathVariable String provider,
                                       @RequestParam MultiValueMap<String, String> queryParams) {
        MultiValueMap<String, String> merged = new LinkedMultiValueMap<>(queryParams);
        String normalizedProvider = provider == null ? "" : provider.toLowerCase(Locale.ROOT);

        if (!merged.containsKey("provider") && !normalizedProvider.isBlank()) {
            merged.add("provider", normalizedProvider);
        }

        String query = UriComponentsBuilder.newInstance()
                .queryParams(merged)
                .build()
                .encode()
                .toUriString();

        if (query == null || query.isBlank()) {
            return "redirect:" + U1_CALLBACK_PATH;
        }
        return "redirect:" + U1_CALLBACK_PATH + query;
    }
}
