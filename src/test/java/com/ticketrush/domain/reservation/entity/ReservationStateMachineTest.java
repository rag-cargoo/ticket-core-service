package com.ticketrush.domain.reservation.entity;

import com.ticketrush.domain.agency.Agency;
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

    private User buildUser() {
        return new User("state-machine-user-" + System.nanoTime());
    }

    private Seat buildSeat(String seatNo) {
        Agency agency = new Agency("agency-" + seatNo + "-" + System.nanoTime());
        Artist artist = new Artist("artist-" + seatNo + "-" + System.nanoTime(), agency);
        Concert concert = new Concert("concert-" + seatNo + "-" + System.nanoTime(), artist);
        ConcertOption option = new ConcertOption(concert, LocalDateTime.now().plusDays(1));
        return new Seat(option, seatNo);
    }
}
