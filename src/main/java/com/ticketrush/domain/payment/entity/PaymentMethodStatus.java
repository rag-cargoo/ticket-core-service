package com.ticketrush.domain.payment.entity;

import java.util.Locale;

public enum PaymentMethodStatus {
    AVAILABLE(true),
    PLANNED(false),
    MAINTENANCE(false),
    DISABLED(false);

    private final boolean enabled;

    PaymentMethodStatus(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static PaymentMethodStatus fromNullable(String rawValue, PaymentMethodStatus fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        String normalized = rawValue.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return PaymentMethodStatus.valueOf(normalized);
    }
}
