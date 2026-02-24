package com.ticketrush.domain.payment.entity;

import java.util.Locale;

public enum PaymentMethod {
    WALLET,
    CARD,
    KAKAOPAY,
    NAVERPAY,
    BANK_TRANSFER;

    public static PaymentMethod fromNullable(String rawValue, PaymentMethod fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }

        String normalized = rawValue.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return PaymentMethod.valueOf(normalized);
    }
}
