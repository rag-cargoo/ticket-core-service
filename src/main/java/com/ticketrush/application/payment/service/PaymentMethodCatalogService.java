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

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentMethodCatalogService implements PaymentMethodCatalogUseCase {

    private static final List<PaymentMethod> CATALOG_METHODS = List.of(
            PaymentMethod.CARD,
            PaymentMethod.KAKAOPAY,
            PaymentMethod.NAVERPAY
    );

    private final PaymentMethodConfigPort paymentConfig;

    @Override
    public PaymentMethodCatalogResult getCatalog() {
        String provider = normalizeProvider(paymentConfig.getProvider());
        List<PaymentMethodStatusResult> methods = catalogMethods(provider).stream()
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
    public void assertMethodAvailable(String paymentMethodCode) {
        PaymentMethod paymentMethod = parsePaymentMethod(paymentMethodCode);
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

    private PaymentMethod parsePaymentMethod(String paymentMethodCode) {
        if (!StringUtils.hasText(paymentMethodCode)) {
            throw new IllegalStateException("Payment method not found: " + paymentMethodCode);
        }

        try {
            return PaymentMethod.valueOf(paymentMethodCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Payment method not found: " + paymentMethodCode, exception);
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
            if ("mock".equals(provider) && method == PaymentMethod.CARD) {
                return "현재 사용 가능 (가상 테스트 카드)";
            }
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
        if (method == PaymentMethod.KAKAOPAY || method == PaymentMethod.NAVERPAY) {
            return PaymentMethodStatus.PLANNED;
        }
        return PaymentMethodStatus.DISABLED;
    }

    private Set<PaymentMethod> supportedMethods(String provider) {
        if ("wallet".equals(provider)) {
            return EnumSet.of(PaymentMethod.WALLET);
        }
        if ("pg-ready".equals(provider) || "mock".equals(provider)) {
            return EnumSet.of(PaymentMethod.CARD);
        }
        return EnumSet.noneOf(PaymentMethod.class);
    }

    private PaymentMethod resolveDefaultMethod(String provider, List<PaymentMethodStatusResult> methods) {
        PaymentMethod preferred = "wallet".equals(provider) ? PaymentMethod.WALLET : PaymentMethod.CARD;
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
            return "mock";
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
            default -> method.name();
        };
    }

    private String resolveProviderMode(String provider) {
        if ("wallet".equals(provider)) {
            return "WALLET_LEDGER";
        }
        if ("mock".equals(provider)) {
            return "CARD_MOCK_SIMULATION";
        }
        if ("pg-ready".equals(provider)) {
            return paymentConfig.isExternalLiveEnabled() ? "PG_EXTERNAL_LIVE" : "PG_WEBHOOK_READY";
        }
        return "CARD_ONLY_RESTRICTED";
    }

    private List<PaymentMethod> catalogMethods(String provider) {
        if ("wallet".equals(provider)) {
            return List.of(PaymentMethod.WALLET);
        }
        return CATALOG_METHODS;
    }
}
