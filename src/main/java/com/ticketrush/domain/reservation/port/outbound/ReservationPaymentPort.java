package com.ticketrush.domain.reservation.port.outbound;

import com.ticketrush.domain.payment.entity.PaymentTransaction;

public interface ReservationPaymentPort {

    PaymentTransaction payForReservation(Long userId, Long reservationId, Long amount, String idempotencyKey);

    PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey);
}
