package com.ticketrush.domain.reservation.port.outbound;

public interface ReservationPaymentPort {

    void payForReservation(Long userId, Long reservationId, Long amount, String idempotencyKey);

    void refundReservation(Long userId, Long reservationId, String idempotencyKey);
}
