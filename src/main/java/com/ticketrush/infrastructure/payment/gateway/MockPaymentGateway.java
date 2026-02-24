package com.ticketrush.infrastructure.payment.gateway;

import com.ticketrush.domain.payment.entity.PaymentMethod;
import com.ticketrush.domain.payment.gateway.PaymentGateway;
import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
import com.ticketrush.domain.payment.repository.PaymentTransactionRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "app.payment", name = "provider", havingValue = "mock")
@RequiredArgsConstructor
public class MockPaymentGateway implements PaymentGateway {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    @Transactional
    public PaymentTransaction payForReservation(
            Long userId,
            Long reservationId,
            Long amount,
            PaymentMethod paymentMethod,
            String idempotencyKey
    ) {
        validateSupportedMethod(paymentMethod);
        String key = normalizeIdempotencyKey(idempotencyKey, "mock-payment-" + reservationId);
        PaymentTransaction existing = findByIdempotencyKey(key);
        if (existing != null) {
            return existing;
        }

        long normalizedAmount = validatePositiveAmount(amount);
        User user = getUser(userId);
        return paymentTransactionRepository.save(PaymentTransaction.payment(
                user,
                reservationId,
                normalizedAmount,
                user.getWalletBalanceAmountSafe(),
                key,
                "MOCK_RESERVATION_PAYMENT",
                PaymentTransactionStatus.SUCCESS,
                paymentMethod,
                provider()
        ));
    }

    @Override
    @Transactional
    public PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey) {
        String key = normalizeIdempotencyKey(idempotencyKey, "mock-refund-" + reservationId);
        PaymentTransaction existing = findByIdempotencyKey(key);
        if (existing != null) {
            return existing;
        }

        PaymentTransaction paidTransaction = paymentTransactionRepository
                .findTopByReservationIdAndTypeAndStatusOrderByIdDesc(
                        reservationId,
                        PaymentTransactionType.PAYMENT,
                        PaymentTransactionStatus.SUCCESS
                )
                .orElseThrow(() -> new IllegalStateException("No successful payment found for reservation: " + reservationId));

        if (!paidTransaction.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Refund user mismatch for reservation: " + reservationId);
        }

        User user = getUser(userId);
        PaymentMethod refundMethod = paidTransaction.getPaymentMethod() == null
                ? PaymentMethod.WALLET
                : paidTransaction.getPaymentMethod();
        String refundProvider = StringUtils.hasText(paidTransaction.getPaymentProvider())
                ? paidTransaction.getPaymentProvider()
                : provider();
        return paymentTransactionRepository.save(PaymentTransaction.refund(
                user,
                reservationId,
                paidTransaction.getAmount(),
                user.getWalletBalanceAmountSafe(),
                key,
                "MOCK_RESERVATION_REFUND",
                refundMethod,
                refundProvider
        ));
    }

    private PaymentTransaction findByIdempotencyKey(String idempotencyKey) {
        return paymentTransactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private long validatePositiveAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return amount;
    }

    private String normalizeIdempotencyKey(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }

    private void validateSupportedMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalStateException("paymentMethod is required");
        }
        switch (paymentMethod) {
            case WALLET, CARD, KAKAOPAY, NAVERPAY, BANK_TRANSFER -> {
            }
            default -> throw new IllegalStateException("Unsupported payment method: " + paymentMethod);
        }
    }
}
