package com.ticketrush.domain.payment.gateway;

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
@ConditionalOnProperty(prefix = "app.payment", name = "provider", havingValue = "pg-ready")
@RequiredArgsConstructor
public class PgReadyPaymentGateway implements PaymentGateway {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PaymentTransaction payForReservation(Long userId, Long reservationId, Long amount, String idempotencyKey) {
        String key = normalizeIdempotencyKey(idempotencyKey, "pg-ready-payment-" + reservationId);
        PaymentTransaction existing = findByIdempotencyKey(key);
        if (existing != null) {
            return existing;
        }

        long normalizedAmount = validatePositiveAmount(amount);
        User user = getUser(userId);

        // PG 연동 전환 전까지는 승인 이벤트를 즉시 성공으로 시뮬레이션한다.
        return paymentTransactionRepository.save(PaymentTransaction.payment(
                user,
                reservationId,
                normalizedAmount,
                user.getWalletBalanceAmountSafe(),
                key,
                "PG_READY_PAYMENT_APPROVED"
        ));
    }

    @Override
    @Transactional
    public PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey) {
        String key = normalizeIdempotencyKey(idempotencyKey, "pg-ready-refund-" + reservationId);
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

        // PG 환불 webhook 정식 연동 전까지는 동기 성공 처리로 고정한다.
        return paymentTransactionRepository.save(PaymentTransaction.refund(
                user,
                reservationId,
                paidTransaction.getAmount(),
                user.getWalletBalanceAmountSafe(),
                key,
                "PG_READY_REFUND_APPROVED"
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
}
