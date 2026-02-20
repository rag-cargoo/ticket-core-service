package com.ticketrush.domain.payment.gateway;

import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PgReadyPaymentGateway.class)
@TestPropertySource(properties = "app.payment.provider=pg-ready")
class PgReadyPaymentGatewayIntegrationTest {

    @jakarta.annotation.Resource
    private PaymentGateway paymentGateway;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @Test
    void payForReservation_shouldCreatePendingPaymentTransaction() {
        User user = userRepository.save(new User("pg-ready-user-" + System.nanoTime()));

        PaymentTransaction tx = paymentGateway.payForReservation(
                user.getId(),
                8001L,
                100_000L,
                "pg-ready-payment-8001"
        );

        assertThat(tx.getType()).isEqualTo(PaymentTransactionType.PAYMENT);
        assertThat(tx.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING);
        assertThat(tx.getDescription()).isEqualTo("PG_READY_PAYMENT_PENDING");
        assertThat(tx.getBalanceAfterAmount()).isEqualTo(user.getWalletBalanceAmountSafe());
    }
}
