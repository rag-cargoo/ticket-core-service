package com.ticketrush.infrastructure.payment.gateway;

import com.ticketrush.domain.payment.entity.PaymentMethod;
import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
import com.ticketrush.domain.payment.gateway.PaymentGateway;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MockPaymentGateway.class)
@TestPropertySource(properties = "app.payment.provider=mock")
class MockPaymentGatewayIntegrationTest {

    @jakarta.annotation.Resource
    private PaymentGateway paymentGateway;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @Test
    void payForReservation_shouldRecordPaymentWithoutChangingWalletBalance() {
        User user = userRepository.save(new User("mock-gateway-user-" + System.nanoTime()));
        long initialBalance = user.getWalletBalanceAmountSafe();

        PaymentTransaction tx = paymentGateway.payForReservation(
                user.getId(),
                7001L,
                80_000L,
                PaymentMethod.CARD,
                "mock-payment-7001"
        );

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(tx.getType()).isEqualTo(PaymentTransactionType.PAYMENT);
        assertThat(tx.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(tx.getPaymentProvider()).isEqualTo("mock");
        assertThat(reloaded.getWalletBalanceAmountSafe()).isEqualTo(initialBalance);
    }

    @Test
    void refundReservation_shouldRecordRefundWithoutChangingWalletBalance() {
        User user = userRepository.save(new User("mock-gateway-refund-user-" + System.nanoTime()));
        long initialBalance = user.getWalletBalanceAmountSafe();
        paymentGateway.payForReservation(user.getId(), 7002L, 90_000L, PaymentMethod.CARD, "mock-payment-7002");

        PaymentTransaction refund = paymentGateway.refundReservation(
                user.getId(),
                7002L,
                "mock-refund-7002"
        );

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refund.getType()).isEqualTo(PaymentTransactionType.REFUND);
        assertThat(refund.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(refund.getPaymentProvider()).isEqualTo("mock");
        assertThat(reloaded.getWalletBalanceAmountSafe()).isEqualTo(initialBalance);
    }
}
