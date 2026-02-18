package com.ticketrush.domain.payment.service;

import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(PaymentServiceImpl.class)
class PaymentServiceIntegrationTest {

    @jakarta.annotation.Resource
    private PaymentService paymentService;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @Test
    void chargeWallet_shouldIncreaseBalanceAndRecordTransaction() {
        User user = userRepository.save(new User("payment-charge-user-" + System.nanoTime()));

        PaymentTransaction tx = paymentService.chargeWallet(
                user.getId(),
                50_000L,
                "charge-" + user.getId(),
                "TEST_CHARGE"
        );

        assertThat(tx.getType()).isEqualTo(PaymentTransactionType.CHARGE);
        assertThat(paymentService.getWalletBalance(user.getId())).isEqualTo(250_000L);
    }

    @Test
    void payForReservation_shouldDecreaseBalanceAndRecordTransaction() {
        User user = userRepository.save(new User("payment-pay-user-" + System.nanoTime()));

        PaymentTransaction tx = paymentService.payForReservation(
                user.getId(),
                101L,
                120_000L,
                "reservation-payment-101"
        );

        assertThat(tx.getType()).isEqualTo(PaymentTransactionType.PAYMENT);
        assertThat(paymentService.getWalletBalance(user.getId())).isEqualTo(80_000L);
    }

    @Test
    void payForReservation_shouldFailWhenBalanceIsInsufficient() {
        User user = userRepository.save(new User("payment-insufficient-user-" + System.nanoTime()));

        assertThatThrownBy(() -> paymentService.payForReservation(
                user.getId(),
                201L,
                300_000L,
                "reservation-payment-201"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient wallet balance");
    }

    @Test
    void refundReservation_shouldRestoreBalanceFromPaymentTransaction() {
        User user = userRepository.save(new User("payment-refund-user-" + System.nanoTime()));
        paymentService.payForReservation(user.getId(), 301L, 110_000L, "reservation-payment-301");

        PaymentTransaction refundTx = paymentService.refundReservation(
                user.getId(),
                301L,
                "reservation-refund-301"
        );

        assertThat(refundTx.getType()).isEqualTo(PaymentTransactionType.REFUND);
        assertThat(paymentService.getWalletBalance(user.getId())).isEqualTo(200_000L);
    }

    @Test
    void chargeWallet_shouldBeIdempotentWithSameKey() {
        User user = userRepository.save(new User("payment-idempotent-user-" + System.nanoTime()));
        String key = "charge-idempotent-" + user.getId();

        PaymentTransaction first = paymentService.chargeWallet(user.getId(), 10_000L, key, "IDEMPOTENT_CHARGE");
        PaymentTransaction second = paymentService.chargeWallet(user.getId(), 10_000L, key, "IDEMPOTENT_CHARGE");

        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(paymentService.getWalletBalance(user.getId())).isEqualTo(210_000L);
    }
}
