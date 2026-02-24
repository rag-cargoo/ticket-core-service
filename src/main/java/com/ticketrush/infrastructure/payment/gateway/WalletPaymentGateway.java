package com.ticketrush.infrastructure.payment.gateway;

import com.ticketrush.application.payment.port.bridge.PaymentGatewayUseCase;
import com.ticketrush.domain.payment.entity.PaymentMethod;
import com.ticketrush.domain.payment.gateway.PaymentGateway;
import com.ticketrush.domain.payment.entity.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.payment", name = "provider", havingValue = "wallet", matchIfMissing = true)
@RequiredArgsConstructor
public class WalletPaymentGateway implements PaymentGateway {

    private final PaymentGatewayUseCase paymentGatewayUseCase;

    @Override
    public String provider() {
        return "wallet";
    }

    @Override
    @Transactional
    public PaymentTransaction payForReservation(
            Long userId,
            Long reservationId,
            Long amount,
            PaymentMethod paymentMethod,
            String idempotencyKey
    ) {
        if (paymentMethod != PaymentMethod.WALLET) {
            throw new IllegalStateException(
                    "Unsupported payment method for wallet provider. requested=" + paymentMethod + ", supported=WALLET"
            );
        }
        return paymentGatewayUseCase.payForReservation(userId, reservationId, amount, idempotencyKey);
    }

    @Override
    @Transactional
    public PaymentTransaction refundReservation(Long userId, Long reservationId, String idempotencyKey) {
        return paymentGatewayUseCase.refundReservation(userId, reservationId, idempotencyKey);
    }
}
