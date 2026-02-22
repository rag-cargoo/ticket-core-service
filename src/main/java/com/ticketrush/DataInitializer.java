package com.ticketrush;

import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.entertainment.Entertainment;
import com.ticketrush.domain.entertainment.EntertainmentRepository;
import com.ticketrush.domain.promoter.Promoter;
import com.ticketrush.domain.promoter.PromoterRepository;
import com.ticketrush.domain.reservation.entity.SalesPolicy;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import com.ticketrush.domain.seed.SeedMarker;
import com.ticketrush.domain.seed.SeedMarkerRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserTier;
import com.ticketrush.domain.venue.Venue;
import com.ticketrush.domain.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * [System] 초기 시스템 데이터 시드
 * - 관리 계정 시드: 기본 활성화
 * - 포트폴리오 샘플 시드: local/demo profile + runtime flag 활성 시에만 적용
 * - idempotent marker 기반으로 1회만 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final int DEFAULT_MAX_RESERVATIONS_PER_USER = 4;
    private static final int DEFAULT_SEAT_COUNT = 20;
    private static final int SOLD_OUT_SEAT_COUNT = 8;
    private static final long DEFAULT_TICKET_PRICE_AMOUNT = 132_000L;

    private final UserRepository userRepository;
    private final SeedMarkerRepository seedMarkerRepository;
    private final EntertainmentRepository entertainmentRepository;
    private final ArtistRepository artistRepository;
    private final PromoterRepository promoterRepository;
    private final VenueRepository venueRepository;
    private final ConcertRepository concertRepository;
    private final ConcertOptionRepository concertOptionRepository;
    private final SeatRepository seatRepository;
    private final SalesPolicyRepository salesPolicyRepository;
    private final Environment environment;

    @Value("${app.seed.create-admin-user:true}")
    private boolean createAdminUser;

    @Value("${app.seed.portfolio-enabled:false}")
    private boolean portfolioSeedEnabled;

    @Value("${app.seed.portfolio-marker-key:portfolio_seed_marker_v1}")
    private String portfolioSeedMarkerKey;

    @Value("${app.seed.portfolio-profiles:local,demo}")
    private String portfolioSeedProfiles;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUserIfNeeded();

        if (!shouldRunPortfolioSeed()) {
            log.info(">>>> Portfolio seed skipped. Set APP_PORTFOLIO_SEED_ENABLED=true on allowed profiles(local,demo) to enable.");
            return;
        }

        String markerKey = normalizeRequired(portfolioSeedMarkerKey, "app.seed.portfolio-marker-key");
        if (seedMarkerRepository.existsByMarkerKey(markerKey)) {
            log.info(">>>> Portfolio seed skipped: marker already exists. marker={}", markerKey);
            return;
        }

        seedPortfolioScenarios();
        seedMarkerRepository.save(new SeedMarker(markerKey, LocalDateTime.now()));
        log.info(">>>> Portfolio seed completed. marker={}", markerKey);
    }

    private void seedAdminUserIfNeeded() {
        if (!createAdminUser) {
            return;
        }
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }
        userRepository.save(new User(ADMIN_USERNAME, UserTier.VIP, UserRole.ADMIN));
        log.info(">>>> Initial data created: admin user registered.");
    }

    private boolean shouldRunPortfolioSeed() {
        if (!portfolioSeedEnabled) {
            return false;
        }
        Set<String> activeProfiles = resolveActiveProfiles();
        Set<String> allowedProfiles = parseProfilesCsv(portfolioSeedProfiles);

        if (allowedProfiles.isEmpty()) {
            log.warn(">>>> Portfolio seed disabled: no allowed profiles configured.");
            return false;
        }

        boolean profileMatched = activeProfiles.stream().anyMatch(allowedProfiles::contains);
        if (!profileMatched) {
            log.info(">>>> Portfolio seed disabled: activeProfiles={} allowedProfiles={}", activeProfiles, allowedProfiles);
        }
        return profileMatched;
    }

    private Set<String> resolveActiveProfiles() {
        String[] active = environment.getActiveProfiles();
        if (active.length == 0) {
            active = environment.getDefaultProfiles();
        }
        Set<String> profiles = new LinkedHashSet<>();
        for (String profile : active) {
            String normalized = normalize(profile);
            if (normalized != null) {
                profiles.add(normalized.toLowerCase(Locale.ROOT));
            }
        }
        return profiles;
    }

    private Set<String> parseProfilesCsv(String csv) {
        Set<String> result = new LinkedHashSet<>();
        if (csv == null) {
            return result;
        }
        Arrays.stream(csv.split(","))
                .map(this::normalize)
                .filter(value -> value != null && !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(result::add);
        return result;
    }

    private void seedPortfolioScenarios() {
        Promoter promoter = upsertPromoter(
                "Portfolio Promoter",
                "KR",
                "https://portfolio-promoter.example.com"
        );
        Venue venue = upsertVenue(
                "Portfolio Dome",
                "Seoul",
                "KR",
                "240 Olympic-ro, Songpa-gu, Seoul"
        );

        LocalDateTime now = LocalDateTime.now();

        seedPortfolioConcert(
                "Portfolio Concert UNSCHEDULED",
                "Portfolio Artist Unscheduled",
                "Portfolio Entertainment Unscheduled",
                promoter,
                venue,
                now.plusDays(10).withHour(20).withMinute(0).withSecond(0).withNano(0),
                null,
                false
        );

        seedPortfolioConcert(
                "Portfolio Concert PREOPEN",
                "Portfolio Artist Preopen",
                "Portfolio Entertainment Preopen",
                promoter,
                venue,
                now.plusDays(11).withHour(20).withMinute(0).withSecond(0).withNano(0),
                now.plusHours(2),
                false
        );

        seedPortfolioConcert(
                "Portfolio Concert OPEN_SOON_1H",
                "Portfolio Artist Soon1H",
                "Portfolio Entertainment Soon1H",
                promoter,
                venue,
                now.plusDays(12).withHour(20).withMinute(0).withSecond(0).withNano(0),
                now.plusMinutes(40),
                false
        );

        seedPortfolioConcert(
                "Portfolio Concert OPEN_SOON_5M",
                "Portfolio Artist Soon5M",
                "Portfolio Entertainment Soon5M",
                promoter,
                venue,
                now.plusDays(13).withHour(20).withMinute(0).withSecond(0).withNano(0),
                now.plusMinutes(3),
                false
        );

        seedPortfolioConcert(
                "Portfolio Concert OPEN",
                "Portfolio Artist Open",
                "Portfolio Entertainment Open",
                promoter,
                venue,
                now.plusDays(14).withHour(20).withMinute(0).withSecond(0).withNano(0),
                now.minusMinutes(30),
                false
        );

        seedPortfolioConcert(
                "Portfolio Concert SOLD_OUT",
                "Portfolio Artist SoldOut",
                "Portfolio Entertainment SoldOut",
                promoter,
                venue,
                now.plusDays(15).withHour(20).withMinute(0).withSecond(0).withNano(0),
                now.minusMinutes(30),
                true
        );
    }

    private void seedPortfolioConcert(
            String title,
            String artistName,
            String entertainmentName,
            Promoter promoter,
            Venue venue,
            LocalDateTime concertDate,
            LocalDateTime generalSaleStartAt,
            boolean soldOut
    ) {
        String normalizedTitle = normalizeRequired(title, "title");
        if (concertRepository.findByTitleIgnoreCase(normalizedTitle).isPresent()) {
            log.info(">>>> Portfolio scenario already exists. title={}", normalizedTitle);
            return;
        }

        Entertainment entertainment = upsertEntertainment(
                entertainmentName,
                "KR",
                "https://" + normalizeHostname(entertainmentName) + ".example.com"
        );
        Artist artist = upsertArtist(
                artistName,
                entertainment,
                artistName,
                "K-POP",
                LocalDate.of(2020, 1, 1)
        );

        Concert concert = concertRepository.save(new Concert(normalizedTitle, artist, promoter));
        ConcertOption option = concertOptionRepository.save(
                new ConcertOption(concert, concertDate, venue, DEFAULT_TICKET_PRICE_AMOUNT)
        );

        int seatCount = soldOut ? SOLD_OUT_SEAT_COUNT : DEFAULT_SEAT_COUNT;
        List<Seat> seats = createSeats(option, seatCount);

        if (generalSaleStartAt != null) {
            salesPolicyRepository.save(
                    SalesPolicy.create(
                            concert,
                            null,
                            null,
                            null,
                            generalSaleStartAt,
                            DEFAULT_MAX_RESERVATIONS_PER_USER
                    )
            );
        }

        if (soldOut) {
            seats.forEach(Seat::reserve);
            seatRepository.saveAll(seats);
        }
    }

    private List<Seat> createSeats(ConcertOption option, int count) {
        List<Seat> seats = new java.util.ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            seats.add(new Seat(option, "A-" + index));
        }
        return seatRepository.saveAll(seats);
    }

    private Entertainment upsertEntertainment(String name, String countryCode, String homepageUrl) {
        String normalizedName = normalizeRequired(name, "entertainmentName");
        return entertainmentRepository.findByNameIgnoreCase(normalizedName)
                .map(existing -> {
                    existing.rename(normalizedName);
                    existing.updateMetadata(countryCode, homepageUrl);
                    return existing;
                })
                .orElseGet(() -> entertainmentRepository.save(
                        new Entertainment(normalizedName, countryCode, homepageUrl)
                ));
    }

    private Artist upsertArtist(
            String name,
            Entertainment entertainment,
            String displayName,
            String genre,
            LocalDate debutDate
    ) {
        String normalizedName = normalizeRequired(name, "artistName");
        return artistRepository.findByNameIgnoreCase(normalizedName)
                .map(existing -> {
                    existing.rename(normalizedName);
                    existing.updateProfile(entertainment, displayName, genre, debutDate);
                    return existing;
                })
                .orElseGet(() -> artistRepository.save(
                        new Artist(normalizedName, entertainment, displayName, genre, debutDate)
                ));
    }

    private Promoter upsertPromoter(String name, String countryCode, String homepageUrl) {
        String normalizedName = normalizeRequired(name, "promoterName");
        return promoterRepository.findByNameIgnoreCase(normalizedName)
                .map(existing -> {
                    existing.update(normalizedName, countryCode, homepageUrl);
                    return existing;
                })
                .orElseGet(() -> promoterRepository.save(
                        new Promoter(normalizedName, countryCode, homepageUrl)
                ));
    }

    private Venue upsertVenue(String name, String city, String countryCode, String address) {
        String normalizedName = normalizeRequired(name, "venueName");
        return venueRepository.findByNameIgnoreCase(normalizedName)
                .map(existing -> {
                    existing.update(normalizedName, city, countryCode, address);
                    return existing;
                })
                .orElseGet(() -> venueRepository.save(
                        new Venue(normalizedName, city, countryCode, address)
                ));
    }

    private String normalizeHostname(String value) {
        String normalized = normalizeRequired(value, "value");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }
}
