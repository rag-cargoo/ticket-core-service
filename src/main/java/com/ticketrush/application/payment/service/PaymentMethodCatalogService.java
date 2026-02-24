package com.ticketrush.application.payment.service;

import com.ticketrush.application.payment.model.PaymentMethodCatalogResult;
import com.ticketrush.application.payment.model.PaymentMethodStatusResult;
import com.ticketrush.application.payment.port.inbound.PaymentMethodCatalogUseCase;
import com.ticketrush.application.payment.port.outbound.PaymentMethodConfigPort;
import com.ticketrush.domain.payment.entity.PaymentMethod;
import com.ticketrush.domain.payment.entity.PaymentMethodStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentMethodCatalogService implements PaymentMethodCatalogUseCase {

    private final PaymentMethodConfigPort paymentConfig;

    @Override
    public PaymentMethodCatalogResult getCatalog() {
        String provider = normalizeProvider(paymentConfig.getProvider());
        List<PaymentMethodStatusResult> methods = Arrays.stream(PaymentMethod.values())
                .map(method -> toMethodStatus(provider, method))
                .toList();

        PaymentMethod defaultMethod = resolveDefaultMethod(provider, methods);
        return new PaymentMethodCatalogResult(
                provider,
                defaultMethod.name(),
                resolveProviderMode(provider),
                paymentConfig.isExternalLiveEnabled(),
                methods
        );
    }

    @Override
    public void assertMethodAvailable(PaymentMethod paymentMethod) {
        PaymentMethodStatusResult method = getCatalog().getMethods().stream()
                .filter(candidate -> candidate.getCode().equals(paymentMethod.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Payment method not found: " + paymentMethod));

        if (!method.isEnabled()) {
            throw new IllegalStateException(
                    "Payment method unavailable. method=" + method.getCode()
                            + ", status=" + method.getStatus()
                            + ", message=" + method.getMessage()
            );
        }
    }

    private PaymentMethodStatusResult toMethodStatus(String provider, PaymentMethod method) {
        PaymentMethodStatus status = resolveMethodStatus(provider, method);
        String message = resolveMethodMessage(provider, method, status);
        return new PaymentMethodStatusResult(
                method.name(),
                toMethodLabel(method),
                status.isEnabled(),
                status.name(),
                message
        );
    }

    private PaymentMethodStatus resolveMethodStatus(String provider, PaymentMethod method) {
        PaymentMethodStatus base = baseStatus(provider, method);
        String overrideRaw = getOverride(paymentConfig.getMethodStatusOverrides(), method);
        if (!StringUtils.hasText(overrideRaw)) {
            return base;
        }
        try {
            return PaymentMethodStatus.fromNullable(overrideRaw, base);
        } catch (IllegalArgumentException exception) {
            return base;
        }
    }

    private String resolveMethodMessage(String provider, PaymentMethod method, PaymentMethodStatus status) {
        String overrideRaw = getOverride(paymentConfig.getMethodMessageOverrides(), method);
        if (StringUtils.hasText(overrideRaw)) {
            return overrideRaw.trim();
        }

        if (status == PaymentMethodStatus.AVAILABLE) {
            if ("pg-ready".equals(provider) && !paymentConfig.isExternalLiveEnabled()) {
                return "승인콜백 준비 단계(실결제 미연동)";
            }
            return "현재 사용 가능";
        }
        if (status == PaymentMethodStatus.MAINTENANCE) {
            return "점검중";
        }
        if (status == PaymentMethodStatus.PLANNED) {
            return "구현 예정";
        }

        Set<PaymentMethod> supported = supportedMethods(provider);
        if (!supported.contains(method)) {
            return "현재 provider에서 미지원";
        }
        return "현재 비활성화";
    }

    private PaymentMethodStatus baseStatus(String provider, PaymentMethod method) {
        Set<PaymentMethod> supported = supportedMethods(provider);
        if (supported.contains(method)) {
            return PaymentMethodStatus.AVAILABLE;
        }

        if ("wallet".equals(provider)) {
            return PaymentMethodStatus.PLANNED;
        }
        if ("pg-ready".equals(provider) && method == PaymentMethod.WALLET) {
            return PaymentMethodStatus.DISABLED;
        }
        return PaymentMethodStatus.DISABLED;
    }

    private Set<PaymentMethod> supportedMethods(String provider) {
        if ("wallet".equals(provider)) {
            return EnumSet.of(PaymentMethod.WALLET);
        }
        if ("pg-ready".equals(provider)) {
            return EnumSet.of(PaymentMethod.CARD, PaymentMethod.KAKAOPAY, PaymentMethod.NAVERPAY, PaymentMethod.BANK_TRANSFER);
        }
        if ("mock".equals(provider)) {
            return EnumSet.allOf(PaymentMethod.class);
        }
        return EnumSet.of(PaymentMethod.WALLET);
    }

    private PaymentMethod resolveDefaultMethod(String provider, List<PaymentMethodStatusResult> methods) {
        PaymentMethod preferred = "pg-ready".equals(provider) ? PaymentMethod.CARD : PaymentMethod.WALLET;
        boolean preferredEnabled = methods.stream()
                .anyMatch(item -> item.getCode().equals(preferred.name()) && item.isEnabled());
        if (preferredEnabled) {
            return preferred;
        }

        return methods.stream()
                .filter(PaymentMethodStatusResult::isEnabled)
                .map(item -> PaymentMethod.valueOf(item.getCode()))
                .findFirst()
                .orElse(preferred);
    }

    private String normalizeProvider(String value) {
        if (!StringUtils.hasText(value)) {
            return "wallet";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String getOverride(Map<String, String> overrides, PaymentMethod method) {
        if (overrides == null || overrides.isEmpty()) {
            return null;
        }
        String codeKey = method.name();
        String lowerKey = method.name().toLowerCase(Locale.ROOT);
        if (overrides.containsKey(codeKey)) {
            return overrides.get(codeKey);
        }
        if (overrides.containsKey(lowerKey)) {
            return overrides.get(lowerKey);
        }
        return null;
    }

    private String toMethodLabel(PaymentMethod method) {
        return switch (method) {
            case WALLET -> "Wallet";
            case CARD -> "카드";
            case KAKAOPAY -> "카카오페이";
            case NAVERPAY -> "네이버페이";
            case BANK_TRANSFER -> "무통장입금";
        };
    }

    private String resolveProviderMode(String provider) {
        if ("wallet".equals(provider)) {
            return "WALLET_LEDGER";
        }
        if ("mock".equals(provider)) {
            return "MOCK_SIMULATION";
        }
        if ("pg-ready".equals(provider)) {
            return paymentConfig.isExternalLiveEnabled() ? "PG_EXTERNAL_LIVE" : "PG_WEBHOOK_READY";
        }
        return "UNKNOWN";
    }
}
