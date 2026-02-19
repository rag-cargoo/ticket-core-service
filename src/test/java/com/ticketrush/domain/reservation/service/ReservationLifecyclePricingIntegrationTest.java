package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
import com.ticketrush.domain.payment.repository.PaymentTransactionRepository;
import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserTier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReservationLifecyclePricingIntegrationTest {

    @Autowired
    private ConcertService concertService;

    @Autowired
    private ReservationLifecycleService reservationLifecycleService;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void confirmUsesConcertOptionTicketPriceWhenConfigured() {
        User user = new User("price_user_" + System.nanoTime(), UserTier.BASIC);
        user.chargeWallet(400_000L);
        user = userRepository.save(user);

        var concert = concertService.createConcert("Price Concert", "Price Artist", "Price Agency");
        var option = concertService.addOption(concert.getId(), LocalDateTime.now().plusDays(1), 350_000L);
        concertService.createSeats(option.getId(), 1);

        Long seatId = concertService.getAvailableSeats(option.getId()).get(0).getId();
        var holdResponse = reservationLifecycleService.createHold(new ReservationRequest(user.getId(), seatId));
        reservationLifecycleService.startPaying(holdResponse.getId(), user.getId());
        reservationLifecycleService.confirm(holdResponse.getId(), user.getId());

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getWalletBalanceAmountSafe()).isEqualTo(250_000L);

        var payment = paymentTransactionRepository.findTopByReservationIdAndTypeAndStatusOrderByIdDesc(
                        holdResponse.getId(),
                        PaymentTransactionType.PAYMENT,
                        PaymentTransactionStatus.SUCCESS
                )
                .orElseThrow();
        assertThat(payment.getAmount()).isEqualTo(350_000L);
    }
}
