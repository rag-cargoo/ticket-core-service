package com.ticketrush;

import com.ticketrush.api.dto.reservation.SalesPolicyUpsertRequest;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.domain.reservation.service.SalesPolicyService;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [System] 초기 시스템 기동 확인용 데이터 이니셜라이저
 * 상세한 테스트 데이터는 /api/concerts/setup API를 통해 동적으로 생성하십시오.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String PORTFOLIO_SEED_MARKER_USERNAME = "portfolio_seed_marker_v1";

    private final UserRepository userRepository;
    private final ConcertService concertService;
    private final SalesPolicyService salesPolicyService;
    private final SeatRepository seatRepository;

    @Value("${app.portfolio-seed.enabled:false}")
    private boolean portfolioSeedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        ensureAdminUser();

        if (!portfolioSeedEnabled) {
            log.info(">>>> Portfolio seed skipped (app.portfolio-seed.enabled=false).");
            log.info(">>>> System is ready. Use APIs for further data setup.");
            return;
        }

        if (userRepository.existsByUsername(PORTFOLIO_SEED_MARKER_USERNAME)) {
            log.info(">>>> Portfolio seed skipped (already applied marker={}).", PORTFOLIO_SEED_MARKER_USERNAME);
            log.info(">>>> System is ready. Use APIs for further data setup.");
            return;
        }

        seedPortfolioCatalog();
        userRepository.save(new User(PORTFOLIO_SEED_MARKER_USERNAME, UserTier.VIP, UserRole.ADMIN));
        log.info(">>>> Portfolio seed completed.");
        log.info(">>>> System is ready. Use APIs for further data setup.");
    }

    private void ensureAdminUser() {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }

        userRepository.save(new User(ADMIN_USERNAME, UserTier.VIP, UserRole.ADMIN));
        log.info(">>>> Initial data created: Admin user registered.");
    }

    private void seedPortfolioCatalog() {
        LocalDateTime now = LocalDateTime.now();

        seedConcert(
                "Portfolio Countdown Arena",
                "Portfolio Star A",
                "Portfolio Entertainment",
                "PORTFOLIO STAR A",
                LocalDate.of(2020, 1, 15),
                LocalDateTime.now().plusDays(14).withHour(20).withMinute(0).withSecond(0).withNano(0),
                now.plusHours(3),
                160,
                false
        );

        seedConcert(
                "Portfolio Prime Time Stage",
                "Portfolio Star B",
                "Portfolio Entertainment",
                "PORTFOLIO STAR B",
                LocalDate.of(2018, 6, 21),
                LocalDateTime.now().plusDays(10).withHour(19).withMinute(30).withSecond(0).withNano(0),
                now.plusMinutes(40),
                120,
                false
        );

        seedConcert(
                "Portfolio Rush Hour Live",
                "Portfolio Star C",
                "Portfolio Entertainment",
                "PORTFOLIO STAR C",
                LocalDate.of(2021, 9, 1),
                LocalDateTime.now().plusDays(7).withHour(18).withMinute(0).withSecond(0).withNano(0),
                now.plusMinutes(4),
                90,
                false
        );

        seedConcert(
                "Portfolio Open Floor",
                "Portfolio Star D",
                "Portfolio Entertainment",
                "PORTFOLIO STAR D",
                LocalDate.of(2017, 3, 10),
                LocalDateTime.now().plusDays(5).withHour(21).withMinute(0).withSecond(0).withNano(0),
                now.minusMinutes(5),
                220,
                false
        );

        seedConcert(
                "Portfolio Soldout Memorial",
                "Portfolio Star E",
                "Portfolio Entertainment",
                "PORTFOLIO STAR E",
                LocalDate.of(2015, 11, 4),
                LocalDateTime.now().plusDays(20).withHour(20).withMinute(0).withSecond(0).withNano(0),
                now.minusHours(1),
                1,
                true
        );
    }

    private void seedConcert(String title,
                             String artistName,
                             String agencyName,
                             String artistDisplayName,
                             LocalDate artistDebutDate,
                             LocalDateTime concertDate,
                             LocalDateTime generalSaleStartAt,
                             int seatCount,
                             boolean soldOut) {
        var concert = concertService.createConcert(
                title,
                artistName,
                agencyName,
                artistDisplayName,
                "K-POP",
                artistDebutDate,
                "KR",
                "https://portfolio.example.com"
        );
        var option = concertService.addOption(concert.getId(), concertDate);
        concertService.createSeats(option.getId(), seatCount);
        salesPolicyService.upsert(
                concert.getId(),
                new SalesPolicyUpsertRequest(
                        null,
                        null,
                        null,
                        generalSaleStartAt,
                        2
                )
        );

        if (soldOut) {
            markSoldOut(option.getId());
        }
    }

    private void markSoldOut(Long optionId) {
        List<Seat> availableSeats = seatRepository.findByConcertOptionIdAndStatus(optionId, Seat.SeatStatus.AVAILABLE);
        if (availableSeats.isEmpty()) {
            return;
        }

        Seat firstSeat = availableSeats.get(0);
        firstSeat.reserve();
        seatRepository.save(firstSeat);
    }
}
