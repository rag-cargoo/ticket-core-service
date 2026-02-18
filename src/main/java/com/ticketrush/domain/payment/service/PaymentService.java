package com.ticketrush.domain.payment.service;

import com.ticketrush.domain.payment.entity.PaymentTransaction;

import java.util.List;

public interface PaymentService {

    PaymentTransaction chargeWallet(Long userId, Long amount, String idempotencyKey, String description);

    PaymentTransaction payForReservation(Long userId, Long reservationId, Long amount, String idempotencyKey);

    PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey);

    long getWalletBalance(Long userId);

    List<PaymentTransaction> getTransactions(Long userId, int limit);
}
