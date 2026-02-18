package com.ticketrush.domain.payment.service;

import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
import com.ticketrush.domain.payment.repository.PaymentTransactionRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PaymentTransaction chargeWallet(Long userId, Long amount, String idempotencyKey, String description) {
        String key = normalizeIdempotencyKey(idempotencyKey, "wallet-charge-" + userId + "-" + System.currentTimeMillis());
        PaymentTransaction existing = findByIdempotencyKey(key);
        if (existing != null) {
            return existing;
        }

        long normalizedAmount = validatePositiveAmount(amount);
        User user = getUserForUpdate(userId);
        user.chargeWallet(normalizedAmount);

        return paymentTransactionRepository.save(PaymentTransaction.charge(
                user,
                normalizedAmount,
                user.getWalletBalanceAmountSafe(),
                key,
                normalizeDescription(description, "WALLET_CHARGE")
        ));
    }

    @Override
    @Transactional
    public PaymentTransaction payForReservation(Long userId, Long reservationId, Long amount, String idempotencyKey) {
        String key = normalizeIdempotencyKey(idempotencyKey, "reservation-payment-" + reservationId);
        PaymentTransaction existing = findByIdempotencyKey(key);
        if (existing != null) {
            return existing;
        }

        long normalizedAmount = validatePositiveAmount(amount);
        User user = getUserForUpdate(userId);
        user.payFromWallet(normalizedAmount);

        return paymentTransactionRepository.save(PaymentTransaction.payment(
                user,
                reservationId,
                normalizedAmount,
                user.getWalletBalanceAmountSafe(),
                key,
                "RESERVATION_PAYMENT"
        ));
    }

    @Override
    @Transactional
    public PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey) {
        String key = normalizeIdempotencyKey(idempotencyKey, "reservation-refund-" + reservationId);
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

        User user = getUserForUpdate(userId);
        user.chargeWallet(paidTransaction.getAmount());

        return paymentTransactionRepository.save(PaymentTransaction.refund(
                user,
                reservationId,
                paidTransaction.getAmount(),
                user.getWalletBalanceAmountSafe(),
                key,
                "RESERVATION_REFUND"
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public long getWalletBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.getWalletBalanceAmountSafe();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> getTransactions(Long userId, int limit) {
        int normalizedLimit = normalizeLimit(limit);
        return paymentTransactionRepository.findByUserIdOrderByIdDesc(userId, PageRequest.of(0, normalizedLimit));
    }

    private PaymentTransaction findByIdempotencyKey(String idempotencyKey) {
        return paymentTransactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
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

    private String normalizeDescription(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
