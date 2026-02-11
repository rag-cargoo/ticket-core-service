package com.ticketrush.domain.user;

import java.util.Arrays;

public enum SocialProvider {
    KAKAO,
    NAVER;

    public static SocialProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("provider is required");
        }
        return Arrays.stream(values())
                .filter(provider -> provider.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + value));
    }
}
