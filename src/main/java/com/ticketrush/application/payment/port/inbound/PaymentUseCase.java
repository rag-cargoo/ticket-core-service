package com.ticketrush.application.payment.port.inbound;

import com.ticketrush.application.payment.model.PaymentTransactionResult;

import java.util.List;

public interface PaymentUseCase {

    long getWalletBalance(Long userId);

    PaymentTransactionResult chargeWalletResult(Long userId, Long amount, String idempotencyKey, String description);

    List<PaymentTransactionResult> getTransactionResults(Long userId, int limit);
}
