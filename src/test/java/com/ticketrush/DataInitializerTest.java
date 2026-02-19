package com.ticketrush;

import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.domain.reservation.service.SalesPolicyService;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    private UserRepository userRepository;
    private ConcertService concertService;
    private SalesPolicyService salesPolicyService;
    private SeatRepository seatRepository;
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        concertService = mock(ConcertService.class);
        salesPolicyService = mock(SalesPolicyService.class);
        seatRepository = mock(SeatRepository.class);
        dataInitializer = new DataInitializer(userRepository, concertService, salesPolicyService, seatRepository);
    }

    @Test
    void runSkipsPortfolioSeedWhenDisabled() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        ReflectionTestUtils.setField(dataInitializer, "portfolioSeedEnabled", false);

        dataInitializer.run();

        verify(userRepository).save(any(User.class)); // admin user
        verify(userRepository, never()).existsByUsername("portfolio_seed_marker_v1");
        verify(concertService, never()).createConcert(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(),
                anyString(),
                anyString()
        );
    }

    @Test
    void runSeedsPortfolioCatalogOnceWhenEnabled() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(userRepository.existsByUsername("portfolio_seed_marker_v1")).thenReturn(false);

        Concert concert = mock(Concert.class);
        when(concert.getId()).thenReturn(1L);
        ConcertOption option = mock(ConcertOption.class);
        when(option.getId()).thenReturn(2L);
        Seat seat = mock(Seat.class);

        when(concertService.createConcert(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(),
                anyString(),
                anyString()
        )).thenReturn(concert);
        when(concertService.addOption(anyLong(), any())).thenReturn(option);
        when(seatRepository.findByConcertOptionIdAndStatus(anyLong(), any())).thenReturn(List.of(seat));

        ReflectionTestUtils.setField(dataInitializer, "portfolioSeedEnabled", true);

        dataInitializer.run();

        verify(concertService).createSeats(2L, 160);
        verify(concertService).createSeats(2L, 120);
        verify(concertService).createSeats(2L, 90);
        verify(concertService).createSeats(2L, 220);
        verify(concertService).createSeats(2L, 1);
        verify(salesPolicyService, times(5)).upsert(anyLong(), any());
        verify(seatRepository).save(any(Seat.class));
        verify(userRepository).save(any(User.class)); // marker user
    }

    @Test
    void runSkipsPortfolioSeedWhenMarkerExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(userRepository.existsByUsername("portfolio_seed_marker_v1")).thenReturn(true);
        ReflectionTestUtils.setField(dataInitializer, "portfolioSeedEnabled", true);

        dataInitializer.run();

        verify(concertService, never()).createConcert(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(),
                anyString(),
                anyString()
        );
        verify(userRepository, never()).save(any(User.class));
    }
}
