package com.ticketrush.application.payment.port.bridge;

import com.ticketrush.domain.payment.entity.PaymentTransaction;

public interface PaymentGatewayUseCase {

    PaymentTransaction payForReservation(Long userId, Long reservationId, Long amount, String idempotencyKey);

    PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey);
}
