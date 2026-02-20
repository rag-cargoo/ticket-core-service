package com.ticketrush.domain.payment.gateway;

import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.payment", name = "provider", havingValue = "wallet", matchIfMissing = true)
@RequiredArgsConstructor
public class WalletPaymentGateway implements PaymentGateway {

    private final PaymentService paymentService;

    @Override
    @Transactional
    public PaymentTransaction payForReservation(Long userId, Long reservationId, Long amount, String idempotencyKey) {
        return paymentService.payForReservation(userId, reservationId, amount, idempotencyKey);
    }

    @Override
    @Transactional
    public PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey) {
        return paymentService.refundReservation(userId, reservationId, idempotencyKey);
    }
}
