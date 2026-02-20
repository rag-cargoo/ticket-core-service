package com.ticketrush.domain.reservation.adapter.outbound;

import com.ticketrush.domain.payment.gateway.PaymentGateway;
import com.ticketrush.domain.reservation.port.outbound.ReservationPaymentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationPaymentPortAdapter implements ReservationPaymentPort {

    private final PaymentGateway paymentGateway;

    @Override
    @Transactional
    public void payForReservation(Long userId, Long reservationId, Long amount, String idempotencyKey) {
        paymentGateway.payForReservation(userId, reservationId, amount, idempotencyKey);
    }

    @Override
    @Transactional
    public void refundReservation(Long userId, Long reservationId, String idempotencyKey) {
        paymentGateway.refundReservation(userId, reservationId, idempotencyKey);
    }
}
