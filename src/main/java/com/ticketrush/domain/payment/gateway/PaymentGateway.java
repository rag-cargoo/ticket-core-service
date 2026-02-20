package com.ticketrush.domain.payment.gateway;

import com.ticketrush.domain.payment.entity.PaymentTransaction;

public interface PaymentGateway {

    PaymentTransaction payForReservation(Long userId, Long reservationId, Long amount, String idempotencyKey);

    PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey);
}
