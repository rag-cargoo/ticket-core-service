package com.ticketrush.domain.payment.webhook;

import com.ticketrush.api.dto.payment.PgReadyWebhookRequest;
import com.ticketrush.api.dto.payment.PgReadyWebhookResponse;
import com.ticketrush.domain.entertainment.Entertainment;
import com.ticketrush.domain.entertainment.EntertainmentRepository;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
import com.ticketrush.domain.payment.repository.PaymentTransactionRepository;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.global.cache.ConcertReadCacheEvictor;
import com.ticketrush.global.push.PushNotifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import({PgReadyWebhookService.class, PgReadyWebhookServiceTest.TestConfig.class})
class PgReadyWebhookServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        PushNotifier pushNotifier() {
            return mock(PushNotifier.class);
        }

        @Bean
        ConcertReadCacheEvictor concertReadCacheEvictor() {
            return mock(ConcertReadCacheEvictor.class);
        }
    }

    @jakarta.annotation.Resource
    private PgReadyWebhookService pgReadyWebhookService;

    @jakarta.annotation.Resource
    private PaymentTransactionRepository paymentTransactionRepository;

    @jakarta.annotation.Resource
    private ReservationRepository reservationRepository;

    @jakarta.annotation.Resource
    private SeatRepository seatRepository;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @jakarta.annotation.Resource
    private EntertainmentRepository entertainmentRepository;

    @jakarta.annotation.Resource
    private ArtistRepository artistRepository;

    @jakarta.annotation.Resource
    private ConcertRepository concertRepository;

    @jakarta.annotation.Resource
    private ConcertOptionRepository concertOptionRepository;

    @jakarta.annotation.Resource
    private PushNotifier pushNotifier;

    @Test
    void handle_shouldConfirmReservationWhenApproved() {
        User user = userRepository.save(new User("pg-webhook-approve-user-" + System.nanoTime()));
        Seat seat = saveSeat("PG-A-101");
        seat.hold();
        Reservation reservation = reservationRepository.save(
                Reservation.hold(user, seat, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5))
        );
        reservation.startPaying(LocalDateTime.now());
        String idempotencyKey = "reservation-payment-" + reservation.getId();
        PaymentTransaction paymentTransaction = paymentTransactionRepository.save(
                PaymentTransaction.payment(
                        user,
                        reservation.getId(),
                        100_000L,
                        user.getWalletBalanceAmountSafe(),
                        idempotencyKey,
                        "PG_READY_PAYMENT_PENDING",
                        PaymentTransactionStatus.PENDING
                )
        );

        PgReadyWebhookResponse response = pgReadyWebhookService.handle(new PgReadyWebhookRequest(
                "evt-approved-1",
                "PAYMENT",
                "APPROVED",
                user.getId(),
                reservation.getId(),
                100_000L,
                idempotencyKey,
                "2026-02-21T05:00:00Z",
                "sig-abc",
                null
        ));

        Reservation updatedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        PaymentTransaction updatedPaymentTransaction = paymentTransactionRepository.findById(paymentTransaction.getId()).orElseThrow();
        Seat updatedSeat = seatRepository.findById(seat.getId()).orElseThrow();

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getMessage()).isEqualTo("applied approved");
        assertThat(updatedReservation.getStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
        assertThat(updatedReservation.getConfirmedAt()).isNotNull();
        assertThat(updatedSeat.getStatus()).isEqualTo(Seat.SeatStatus.RESERVED);
        assertThat(updatedPaymentTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCESS);
        assertThat(updatedPaymentTransaction.getDescription()).contains("PG_READY_PAYMENT_APPROVED");
        verify(pushNotifier).sendReservationStatus(user.getId(), seat.getId(), Reservation.ReservationStatus.CONFIRMED.name());
    }

    @Test
    void handle_shouldKeepPayingWhenFailed() {
        User user = userRepository.save(new User("pg-webhook-failed-user-" + System.nanoTime()));
        Seat seat = saveSeat("PG-A-102");
        seat.hold();
        Reservation reservation = reservationRepository.save(
                Reservation.hold(user, seat, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5))
        );
        reservation.startPaying(LocalDateTime.now());
        String idempotencyKey = "reservation-payment-" + reservation.getId();
        PaymentTransaction paymentTransaction = paymentTransactionRepository.save(
                PaymentTransaction.payment(
                        user,
                        reservation.getId(),
                        100_000L,
                        user.getWalletBalanceAmountSafe(),
                        idempotencyKey,
                        "PG_READY_PAYMENT_PENDING",
                        PaymentTransactionStatus.PENDING
                )
        );

        PgReadyWebhookResponse response = pgReadyWebhookService.handle(new PgReadyWebhookRequest(
                "evt-failed-1",
                "PAYMENT",
                "FAILED",
                user.getId(),
                reservation.getId(),
                100_000L,
                idempotencyKey,
                "2026-02-21T05:01:00Z",
                "sig-def",
                null
        ));

        Reservation updatedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        PaymentTransaction updatedPaymentTransaction = paymentTransactionRepository.findById(paymentTransaction.getId()).orElseThrow();
        Seat updatedSeat = seatRepository.findById(seat.getId()).orElseThrow();

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getMessage()).isEqualTo("applied failed");
        assertThat(updatedReservation.getStatus()).isEqualTo(Reservation.ReservationStatus.PAYING);
        assertThat(updatedReservation.getConfirmedAt()).isNull();
        assertThat(updatedSeat.getStatus()).isEqualTo(Seat.SeatStatus.TEMP_RESERVED);
        assertThat(updatedPaymentTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
        assertThat(updatedPaymentTransaction.getDescription()).contains("PG_READY_PAYMENT_FAILED");
    }

    @Test
    void handle_shouldIgnoreUnsupportedEventType() {
        PgReadyWebhookResponse response = pgReadyWebhookService.handle(new PgReadyWebhookRequest(
                "evt-unsupported",
                "REFUND",
                "APPROVED",
                1001L,
                9001L,
                100_000L,
                null,
                "2026-02-21T05:00:00Z",
                "sig-abc",
                null
        ));

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getMessage()).isEqualTo("ignored unsupported eventType");
    }

    private Seat saveSeat(String seatNumber) {
        Entertainment entertainment = entertainmentRepository.save(new Entertainment("pg-webhook-entertainment-" + seatNumber + "-" + System.nanoTime()));
        Artist artist = artistRepository.save(new Artist("pg-webhook-artist-" + seatNumber + "-" + System.nanoTime(), entertainment));
        Concert concert = concertRepository.save(new Concert("pg-webhook-concert-" + seatNumber + "-" + System.nanoTime(), artist));
        ConcertOption option = concertOptionRepository.save(new ConcertOption(concert, LocalDateTime.now().plusDays(3)));
        return seatRepository.save(new Seat(option, seatNumber));
    }
}
