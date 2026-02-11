package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.reservation.ReservationLifecycleResponse;
import com.ticketrush.domain.agency.Agency;
import com.ticketrush.domain.agency.AgencyRepository;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.global.config.ReservationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        ReservationLifecycleService.class,
        ReservationLifecycleServiceIntegrationTest.TestConfig.class
})
class ReservationLifecycleServiceIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ReservationProperties reservationProperties() {
            ReservationProperties properties = new ReservationProperties();
            properties.setHoldTtlSeconds(1);
            properties.setExpireCheckDelayMillis(1000);
            return properties;
        }
    }

    @jakarta.annotation.Resource
    private ReservationLifecycleService reservationLifecycleService;

    @jakarta.annotation.Resource
    private ReservationRepository reservationRepository;

    @jakarta.annotation.Resource
    private SeatRepository seatRepository;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @jakarta.annotation.Resource
    private AgencyRepository agencyRepository;

    @jakarta.annotation.Resource
    private ArtistRepository artistRepository;

    @jakarta.annotation.Resource
    private ConcertRepository concertRepository;

    @jakarta.annotation.Resource
    private ConcertOptionRepository concertOptionRepository;

    @Test
    void holdToPayingToConfirmed_shouldReserveSeat() {
        User user = userRepository.save(new User("step9-integration-user-" + System.nanoTime()));
        Seat seat = saveSeat("A-101");

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );
        assertThat(hold.getStatus()).isEqualTo(Reservation.ReservationStatus.HOLD.name());
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
                .isEqualTo(Seat.SeatStatus.TEMP_RESERVED);

        ReservationLifecycleResponse paying = reservationLifecycleService.startPaying(hold.getId(), user.getId());
        assertThat(paying.getStatus()).isEqualTo(Reservation.ReservationStatus.PAYING.name());

        ReservationLifecycleResponse confirmed = reservationLifecycleService.confirm(hold.getId(), user.getId());
        assertThat(confirmed.getStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED.name());
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
                .isEqualTo(Seat.SeatStatus.RESERVED);
    }

    @Test
    void expireTimedOutHolds_shouldExpireAndReleaseSeat() throws Exception {
        User user = userRepository.save(new User("step9-expire-user-" + System.nanoTime()));
        Seat seat = saveSeat("A-102");

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );

        reservationRepository.updateHoldExpiresAt(hold.getId(), LocalDateTime.now().minusSeconds(1));
        int expiredCount = reservationLifecycleService.expireTimedOutHolds();

        Reservation expired = reservationRepository.findById(hold.getId()).orElseThrow();
        assertThat(expiredCount).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(Reservation.ReservationStatus.EXPIRED);
        assertThat(expired.getExpiredAt()).isNotNull();
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
                .isEqualTo(Seat.SeatStatus.AVAILABLE);
    }

    private Seat saveSeat(String seatNo) {
        Agency agency = agencyRepository.save(new Agency("agency-" + seatNo + "-" + System.nanoTime()));
        Artist artist = artistRepository.save(new Artist("artist-" + seatNo + "-" + System.nanoTime(), agency));
        Concert concert = concertRepository.save(new Concert("concert-" + seatNo + "-" + System.nanoTime(), artist));
        ConcertOption option = concertOptionRepository.save(new ConcertOption(concert, java.time.LocalDateTime.now().plusDays(1)));
        return seatRepository.save(new Seat(option, seatNo));
    }
}
