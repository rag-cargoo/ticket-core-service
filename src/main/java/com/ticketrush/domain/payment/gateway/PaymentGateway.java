package com.ticketrush.domain.payment.gateway;

import com.ticketrush.domain.payment.entity.PaymentMethod;
import com.ticketrush.domain.payment.entity.PaymentTransaction;

public interface PaymentGateway {

    String provider();

    PaymentTransaction payForReservation(
            Long userId,
            Long reservationId,
            Long amount,
            PaymentMethod paymentMethod,
            String idempotencyKey
    );

    PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey);
}
