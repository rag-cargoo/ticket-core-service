package com.ticketrush.domain.reservation.entity;

import com.ticketrush.domain.entertainment.Entertainment;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ReservationStateMachineTest {

    @Test
    void holdToPayingToConfirmed() {
        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = Reservation.hold(buildUser(), buildSeat("A-1"), now, now.plusMinutes(5));

        reservation.startPaying(now.plusSeconds(10));
        reservation.confirmPayment(now.plusSeconds(20));

        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
        assertThat(reservation.getConfirmedAt()).isNotNull();
        assertThat(reservation.getHoldExpiresAt()).isNull();
    }

    @Test
    void confirmWithoutPayingShouldFail() {
        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = Reservation.hold(buildUser(), buildSeat("A-2"), now, now.plusMinutes(5));

        assertThatIllegalStateException()
                .isThrownBy(() -> reservation.confirmPayment(now.plusSeconds(1)))
                .withMessageContaining("Only PAYING reservation can transition to CONFIRMED.");
    }

    @Test
    void startPayingAfterExpiryShouldFail() {
        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = Reservation.hold(buildUser(), buildSeat("A-3"), now, now.plusSeconds(1));

        assertThatIllegalStateException()
                .isThrownBy(() -> reservation.startPaying(now.plusSeconds(2)))
                .withMessageContaining("Reservation hold has expired.");
    }

    @Test
    void expireFromHoldShouldSetExpired() {
        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = Reservation.hold(buildUser(), buildSeat("A-4"), now, now.plusSeconds(1));

        reservation.expire(now.plusSeconds(2));

        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.EXPIRED);
        assertThat(reservation.getExpiredAt()).isNotNull();
        assertThat(reservation.getHoldExpiresAt()).isNull();
    }

    @Test
    void confirmedToCancelledToRefunded() {
        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = Reservation.hold(buildUser(), buildSeat("A-5"), now, now.plusMinutes(5));
        reservation.startPaying(now.plusSeconds(10));
        reservation.confirmPayment(now.plusSeconds(20));

        reservation.cancel(now.plusSeconds(30));
        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.CANCELLED);
        assertThat(reservation.getCancelledAt()).isNotNull();

        reservation.refund(now.plusSeconds(40));
        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.REFUNDED);
        assertThat(reservation.getRefundedAt()).isNotNull();
    }

    @Test
    void cancelFromPayingShouldFail() {
        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = Reservation.hold(buildUser(), buildSeat("A-6"), now, now.plusMinutes(5));
        reservation.startPaying(now.plusSeconds(10));

        assertThatIllegalStateException()
                .isThrownBy(() -> reservation.cancel(now.plusSeconds(20)))
                .withMessageContaining("Only CONFIRMED reservation can transition to CANCELLED.");
    }

    private User buildUser() {
        return new User("state-machine-user-" + System.nanoTime());
    }

    private Seat buildSeat(String seatNo) {
        Entertainment entertainment = new Entertainment("entertainment-" + seatNo + "-" + System.nanoTime());
        Artist artist = new Artist("artist-" + seatNo + "-" + System.nanoTime(), entertainment);
        Concert concert = new Concert("concert-" + seatNo + "-" + System.nanoTime(), artist);
        ConcertOption option = new ConcertOption(concert, LocalDateTime.now().plusDays(1));
        return new Seat(option, seatNo);
    }
}
