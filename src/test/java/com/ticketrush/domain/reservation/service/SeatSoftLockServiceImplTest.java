package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.entertainment.Entertainment;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.port.outbound.ReservationSeatPort;
import com.ticketrush.global.config.ReservationProperties;
import com.ticketrush.global.push.PushNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatSoftLockServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ReservationSeatPort reservationSeatPort;

    @Mock
    private PushNotifier pushNotifier;

    private SeatSoftLockServiceImpl seatSoftLockService;

    @BeforeEach
    void setUp() {
        ReservationProperties reservationProperties = new ReservationProperties();
        reservationProperties.setSoftLockTtlSeconds(30);
        reservationProperties.setSoftLockKeyPrefix("seat:lock:");

        seatSoftLockService = new SeatSoftLockServiceImpl(
                redisTemplate,
                reservationSeatPort,
                reservationProperties,
                pushNotifier
        );

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(reservationSeatPort.getSeat(55L)).thenReturn(buildSeat(11L, 55L));
    }

    @Test
    void acquire_whenKeyIsEmpty_setsNxAndBroadcastsSelecting() {
        when(valueOperations.setIfAbsent(
                eq("seat:lock:11:55"),
                anyString(),
                eq(30L),
                eq(TimeUnit.SECONDS)
        )).thenReturn(true);

        SeatSoftLockService.SeatSoftLockAcquireResult result = seatSoftLockService.acquire(200L, 55L, "req-1");

        assertThat(result.optionId()).isEqualTo(11L);
        assertThat(result.seatId()).isEqualTo(55L);
        assertThat(result.ownerUserId()).isEqualTo(200L);
        assertThat(result.status()).isEqualTo("SELECTING");
        assertThat(result.requestId()).isEqualTo("req-1");
        assertThat(result.expiresAt()).isNotBlank();
        assertThat(result.ttlSeconds()).isEqualTo(30L);
        verify(pushNotifier).sendSeatMapStatus(11L, 55L, "SELECTING", 200L, result.expiresAt());
    }

    @Test
    void ensureHoldableByUser_whenForeignLockExists_throwsConflict() {
        when(valueOperations.get("seat:lock:11:55")).thenReturn("999|req-x|2026-02-22T02:00:00Z");

        assertThatThrownBy(() -> seatSoftLockService.ensureHoldableByUser(200L, 55L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another user");
    }

    @Test
    void release_whenOwnerMatches_deletesKeyAndBroadcastsReleased() {
        when(valueOperations.get("seat:lock:11:55")).thenReturn("200|req-x|2026-02-22T02:00:00Z");

        SeatSoftLockService.SeatSoftLockReleaseResult result = seatSoftLockService.release(200L, 55L);

        assertThat(result.optionId()).isEqualTo(11L);
        assertThat(result.seatId()).isEqualTo(55L);
        assertThat(result.status()).isEqualTo("RELEASED");
        assertThat(result.released()).isTrue();
        verify(redisTemplate).delete("seat:lock:11:55");
        verify(pushNotifier).sendSeatMapStatus(11L, 55L, "RELEASED", null, null);
    }

    @Test
    void promoteToHold_clearsSoftLockAndBroadcastsHold() {
        LocalDateTime holdExpiresAt = LocalDateTime.of(2026, 2, 22, 2, 5, 0);

        seatSoftLockService.promoteToHold(200L, 55L, holdExpiresAt);

        verify(redisTemplate).delete("seat:lock:11:55");
        verify(pushNotifier).sendSeatMapStatus(
                eq(11L),
                eq(55L),
                eq("HOLD"),
                eq(200L),
                anyString()
        );
    }

    private Seat buildSeat(Long optionId, Long seatId) {
        Entertainment entertainment = new Entertainment("soft-lock-entertainment-" + seatId);
        Artist artist = new Artist("soft-lock-artist-" + seatId, entertainment);
        Concert concert = new Concert("soft-lock-concert-" + seatId, artist);
        ConcertOption option = new ConcertOption(concert, LocalDateTime.now().plusDays(1));
        Seat seat = new Seat(option, "A-" + seatId);

        ReflectionTestUtils.setField(option, "id", optionId);
        ReflectionTestUtils.setField(seat, "id", seatId);
        return seat;
    }
}
