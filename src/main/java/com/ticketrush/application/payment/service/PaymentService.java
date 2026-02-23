package com.ticketrush.application.payment.service;

import com.ticketrush.application.payment.port.bridge.PaymentGatewayUseCase;
import com.ticketrush.application.payment.port.inbound.PaymentUseCase;
import com.ticketrush.domain.payment.entity.PaymentTransaction;

import java.util.List;

public interface PaymentService extends PaymentUseCase, PaymentGatewayUseCase {

    PaymentTransaction chargeWallet(Long userId, Long amount, String idempotencyKey, String description);

    List<PaymentTransaction> getTransactions(Long userId, int limit);
}
