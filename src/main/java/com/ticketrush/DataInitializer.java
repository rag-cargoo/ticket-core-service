package com.ticketrush;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * [System] 초기 시스템 데이터 시드
 * - 관리 계정 시드: 기본 활성화
 * - 포트폴리오 샘플 시드: local/demo profile + runtime flag 활성 시에만 적용
 * - KPOP20 데모 시드: JSON dataset + runtime flag 활성 시에만 적용
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
    private static final int DEFAULT_OPTION_COUNT_PER_CONCERT = 2;
    private static final long DEFAULT_TICKET_PRICE_AMOUNT = 132_000L;
    private static final String KPOP_DEMO_PROMOTER = "KPOP LIVE PROMOTIONS";
    private static final String KPOP_DEMO_VENUE = "KPOP ARENA SEOUL";
    private static final String KPOP_TITLE_BASE = " LIVE IN SEOUL";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final byte[] FALLBACK_THUMBNAIL_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIW2NkYGD4DwABBAEAQ0MyoQAAAABJRU5ErkJggg=="
    );
    private static final String FALLBACK_THUMBNAIL_CONTENT_TYPE = "image/png";

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
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${app.seed.create-admin-user:true}")
    private boolean createAdminUser;

    @Value("${app.seed.portfolio-enabled:false}")
    private boolean portfolioSeedEnabled;

    @Value("${app.seed.portfolio-marker-key:portfolio_seed_marker_v1}")
    private String portfolioSeedMarkerKey;

    @Value("${app.seed.portfolio-profiles:local,demo}")
    private String portfolioSeedProfiles;

    @Value("${app.seed.kpop20-enabled:false}")
    private boolean kpop20SeedEnabled;

    @Value("${app.seed.kpop20-marker-key:kpop20_seed_marker_v1}")
    private String kpop20SeedMarkerKey;

    @Value("${app.seed.kpop20-profiles:local,demo}")
    private String kpop20SeedProfiles;

    @Value("${app.seed.kpop20-dataset-resource:classpath:seed/kpop20-demo-dataset.json}")
    private String kpop20DatasetResource;

    @Value("${app.seed.kpop20-title-tag:KPOP20}")
    private String kpop20TitleTag;

    @Value("${app.seed.kpop20-max-reservations-per-user:8}")
    private int kpop20MaxReservationsPerUser;

    @Value("${app.seed.kpop20-ticket-price-amount:132000}")
    private long kpop20TicketPriceAmount;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUserIfNeeded();
        seedPortfolioIfNeeded();
        seedKpop20IfNeeded();
    }

    private void seedPortfolioIfNeeded() {
        if (!shouldRunSeed(portfolioSeedEnabled, portfolioSeedProfiles, "Portfolio")) {
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

    private void seedKpop20IfNeeded() {
        if (!shouldRunSeed(kpop20SeedEnabled, kpop20SeedProfiles, "KPOP20")) {
            log.info(">>>> KPOP20 seed skipped. Set APP_SEED_KPOP20_ENABLED=true on allowed profiles(local,demo) to enable.");
            return;
        }

        String markerKey = normalizeRequired(kpop20SeedMarkerKey, "app.seed.kpop20-marker-key");
        if (seedMarkerRepository.existsByMarkerKey(markerKey)) {
            log.info(">>>> KPOP20 seed skipped: marker already exists. marker={}", markerKey);
            return;
        }

        List<KpopConcertSeedItem> dataset = loadKpopDataset();
        seedKpop20Concerts(dataset);
        seedMarkerRepository.save(new SeedMarker(markerKey, LocalDateTime.now()));
        log.info(">>>> KPOP20 seed completed. marker={} rows={}", markerKey, dataset.size());
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

    private boolean shouldRunSeed(boolean enabled, String allowedProfilesCsv, String seedName) {
        if (!enabled) {
            return false;
        }
        Set<String> activeProfiles = resolveActiveProfiles();
        Set<String> allowedProfiles = parseProfilesCsv(allowedProfilesCsv);

        if (allowedProfiles.isEmpty()) {
            log.warn(">>>> {} seed disabled: no allowed profiles configured.", seedName);
            return false;
        }

        boolean profileMatched = activeProfiles.stream().anyMatch(allowedProfiles::contains);
        if (!profileMatched) {
            log.info(">>>> {} seed disabled: activeProfiles={} allowedProfiles={}", seedName, activeProfiles, allowedProfiles);
        }
        return profileMatched;
    }

    private List<KpopConcertSeedItem> loadKpopDataset() {
        String location = normalizeRequired(kpop20DatasetResource, "app.seed.kpop20-dataset-resource");
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("KPOP20 dataset resource not found: " + location);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            List<KpopConcertSeedItem> rows = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<KpopConcertSeedItem>>() {
                    }
            );
            if (rows == null || rows.isEmpty()) {
                throw new IllegalStateException("KPOP20 dataset is empty: " + location);
            }
            for (int index = 0; index < rows.size(); index++) {
                validateKpopSeedItem(rows.get(index), index, location);
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load KPOP20 dataset: " + location, exception);
        }
    }

    private void validateKpopSeedItem(KpopConcertSeedItem row, int index, String location) {
        if (row == null) {
            throw new IllegalStateException("Invalid KPOP20 dataset row at index " + index + ": row is null (" + location + ")");
        }
        if (normalize(row.artist) == null
                || normalize(row.entertainment) == null
                || normalize(row.youtubeUrl) == null
                || row.seatCount == null
                || row.seatCount <= 0
                || normalize(row.saleBucket) == null) {
            throw new IllegalStateException(
                    "Invalid KPOP20 dataset row at index " + index
                            + ": artist/entertainment/youtubeUrl/seatCount/saleBucket required (" + location + ")"
            );
        }
        try {
            KpopSaleBucket.from(row.saleBucket);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Invalid KPOP20 dataset row at index " + index
                            + ": unsupported saleBucket=" + row.saleBucket + " (" + location + ")",
                    exception
            );
        }
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

    private void seedKpop20Concerts(List<KpopConcertSeedItem> rows) {
        Promoter promoter = upsertPromoter(
                KPOP_DEMO_PROMOTER,
                "KR",
                "https://kpop-live-promotions.example.com"
        );
        Venue venue = upsertVenue(
                KPOP_DEMO_VENUE,
                "Seoul",
                "KR",
                "240 Olympic-ro, Songpa-gu, Seoul"
        );

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        int seededCount = 0;

        for (int index = 0; index < rows.size(); index++) {
            KpopConcertSeedItem row = rows.get(index);
            String artistName = normalizeRequired(row.artist, "artist");
            String entertainmentName = normalizeRequired(row.entertainment, "entertainment");
            String youtubeUrl = normalizeRequired(row.youtubeUrl, "youtubeUrl");
            KpopSaleBucket saleBucket = KpopSaleBucket.from(row.saleBucket);
            String concertTitle = buildKpopConcertTitle(artistName);

            if (concertRepository.findByTitleIgnoreCase(concertTitle).isPresent()) {
                log.info(">>>> KPOP20 seed skipped existing concert. title={}", concertTitle);
                continue;
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
            Concert concert = concertRepository.save(new Concert(concertTitle, artist, promoter, youtubeUrl));
            applyKpopThumbnail(concert, youtubeUrl);

            LocalDateTime concertDate = now.plusDays(20L + index).withHour(19).withMinute(0).withSecond(0).withNano(0);
            ConcertOption option = concertOptionRepository.save(
                    new ConcertOption(concert, concertDate, venue, Math.max(0L, kpop20TicketPriceAmount))
            );

            List<Seat> seats = createKpopSeats(option, row.seatCount);
            applyKpopSalesPolicy(concert, saleBucket, artistName, now, index);
            applyKpopSeatOccupancy(seats, saleBucket, artistName, index);
            seededCount++;
        }

        log.info(">>>> KPOP20 seed rows processed={} created={}", rows.size(), seededCount);
    }

    private String buildKpopConcertTitle(String artistName) {
        String base = normalizeRequired(artistName, "artistName") + KPOP_TITLE_BASE;
        String tag = normalize(kpop20TitleTag);
        if (tag == null) {
            return base;
        }
        if (base.contains(tag)) {
            return base;
        }
        return base + " " + tag;
    }

    private List<Seat> createKpopSeats(ConcertOption option, int totalCount) {
        int safeTotal = Math.max(1, totalCount);
        int sectionACount = Math.max(1, Math.round(safeTotal * 18f / 100f));
        int sectionBCount = Math.max(1, Math.round(safeTotal * 37f / 100f));
        if (sectionACount + sectionBCount >= safeTotal) {
            sectionBCount = Math.max(1, safeTotal - sectionACount - 1);
            if (sectionACount + sectionBCount >= safeTotal) {
                sectionACount = Math.max(1, safeTotal - 2);
                sectionBCount = 1;
            }
        }
        int sectionCCount = Math.max(1, safeTotal - sectionACount - sectionBCount);

        List<Seat> seats = new java.util.ArrayList<>(safeTotal);
        appendSectionSeats(seats, option, "A", sectionACount);
        appendSectionSeats(seats, option, "B", sectionBCount);
        appendSectionSeats(seats, option, "C", sectionCCount);
        return seatRepository.saveAll(seats);
    }

    private void appendSectionSeats(List<Seat> seats, ConcertOption option, String sectionCode, int count) {
        for (int index = 1; index <= count; index++) {
            seats.add(new Seat(option, sectionCode + "-" + index));
        }
    }

    private void applyKpopSalesPolicy(
            Concert concert,
            KpopSaleBucket saleBucket,
            String artistName,
            LocalDateTime baseTime,
            int indexSeed
    ) {
        if (saleBucket == KpopSaleBucket.UNSCHEDULED) {
            return;
        }
        LocalDateTime generalSaleStartAt = resolveKpopSaleStartAt(saleBucket, artistName, baseTime, indexSeed);
        salesPolicyRepository.save(
                SalesPolicy.create(
                        concert,
                        null,
                        null,
                        null,
                        generalSaleStartAt,
                        Math.max(1, kpop20MaxReservationsPerUser)
                )
        );
    }

    private LocalDateTime resolveKpopSaleStartAt(
            KpopSaleBucket saleBucket,
            String artistName,
            LocalDateTime baseTime,
            int indexSeed
    ) {
        return switch (saleBucket) {
            case OPEN, SOLD_OUT -> baseTime.minusMinutes(30L + (indexSeed % 30L));
            case OPEN_SOON_5M -> baseTime.plusMinutes(3L).plusSeconds((indexSeed * 5L) % 30L);
            case OPEN_SOON_1H -> {
                String normalizedArtist = normalizeArtistKey(artistName);
                if ("newjeans".equals(normalizedArtist)) {
                    yield baseTime.plusMinutes(16L);
                }
                if ("le sserafim".equals(normalizedArtist)) {
                    yield baseTime.plusMinutes(28L);
                }
                if ("tomorrow x together".equals(normalizedArtist)) {
                    yield baseTime.plusMinutes(40L);
                }
                yield baseTime.plusMinutes(18L + ((indexSeed * 7L) % 36L));
            }
            case PREOPEN -> baseTime.plusHours(3L + (indexSeed % 4L)).plusMinutes((indexSeed * 11L) % 50L);
            case UNSCHEDULED -> throw new IllegalStateException("UNSCHEDULED does not have sale start");
        };
    }

    private void applyKpopSeatOccupancy(
            List<Seat> seats,
            KpopSaleBucket saleBucket,
            String artistName,
            int indexSeed
    ) {
        if (seats == null || seats.isEmpty()) {
            return;
        }
        List<Long> seatIds = seats.stream()
                .map(Seat::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (seatIds.isEmpty()) {
            return;
        }

        if (saleBucket == KpopSaleBucket.SOLD_OUT) {
            seatRepository.updateStatusBySeatIds(seatIds, Seat.SeatStatus.RESERVED);
            return;
        }
        if (saleBucket != KpopSaleBucket.OPEN) {
            return;
        }

        SeatOccupancy occupancy = resolveOpenSeatOccupancy(seatIds.size(), artistName, indexSeed);
        if (occupancy.totalOccupied() <= 0) {
            return;
        }

        List<Long> shuffled = new java.util.ArrayList<>(seatIds);
        Collections.shuffle(shuffled, new Random(Math.abs((artistName + indexSeed).hashCode())));

        int reservedCount = Math.min(occupancy.reservedCount(), shuffled.size());
        int tempReservedCount = Math.min(
                occupancy.tempReservedCount(),
                Math.max(0, shuffled.size() - reservedCount)
        );
        if (reservedCount > 0) {
            seatRepository.updateStatusBySeatIds(
                    shuffled.subList(0, reservedCount),
                    Seat.SeatStatus.RESERVED
            );
        }
        if (tempReservedCount > 0) {
            int from = reservedCount;
            int to = reservedCount + tempReservedCount;
            seatRepository.updateStatusBySeatIds(
                    shuffled.subList(from, to),
                    Seat.SeatStatus.TEMP_RESERVED
            );
        }
    }

    private SeatOccupancy resolveOpenSeatOccupancy(int seatCount, String artistName, int indexSeed) {
        int total = Math.max(1, seatCount);
        String normalizedArtist = normalizeArtistKey(artistName);

        int occupied;
        if ("bts".equals(normalizedArtist)) {
            occupied = Math.round(total * 68f / 100f);
        } else if ("saja boys".equals(normalizedArtist)) {
            occupied = Math.round(total * 62f / 100f);
        } else if ("blackpink".equals(normalizedArtist)) {
            occupied = Math.round(total * 56f / 100f);
        } else {
            int min = Math.round(total * 30f / 100f);
            int max = Math.round(total * 48f / 100f);
            int spread = Math.max(0, max - min);
            occupied = min + Math.abs(indexSeed * 17 % (spread + 1));
        }
        occupied = Math.max(1, Math.min(total - 1, occupied));

        int tempReserved = Math.max(1, occupied / 6);
        tempReserved = Math.min(tempReserved, Math.max(0, occupied - 1));
        int reserved = Math.max(0, occupied - tempReserved);
        return new SeatOccupancy(reserved, tempReserved);
    }

    private void applyKpopThumbnail(Concert concert, String youtubeUrl) {
        ThumbnailPayload payload = resolveYoutubeThumbnail(youtubeUrl)
                .orElseGet(() -> new ThumbnailPayload(FALLBACK_THUMBNAIL_BYTES, FALLBACK_THUMBNAIL_CONTENT_TYPE));
        concert.updateThumbnail(payload.bytes(), payload.contentType(), LocalDateTime.now());
    }

    private Optional<ThumbnailPayload> resolveYoutubeThumbnail(String youtubeUrl) {
        String videoId = extractYoutubeVideoId(youtubeUrl);
        if (videoId == null) {
            return Optional.empty();
        }
        List<String> candidates = List.of(
                "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg",
                "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg",
                "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg"
        );
        for (String candidate : candidates) {
            Optional<ThumbnailPayload> payload = downloadImage(candidate);
            if (payload.isPresent()) {
                return payload;
            }
        }
        return Optional.empty();
    }

    private Optional<ThumbnailPayload> downloadImage(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "image/*")
                    .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                return Optional.empty();
            }
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .map(String::trim)
                    .orElse("image/jpeg");
            if (!contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return Optional.empty();
            }
            return Optional.of(new ThumbnailPayload(body, contentType));
        } catch (Exception exception) {
            log.debug(">>>> KPOP20 thumbnail download failed: {} ({})", imageUrl, exception.getMessage());
            return Optional.empty();
        }
    }

    private String extractYoutubeVideoId(String youtubeUrl) {
        String normalized = normalize(youtubeUrl);
        if (normalized == null) {
            return null;
        }
        String watchToken = "v=";
        int watchIndex = normalized.indexOf(watchToken);
        if (watchIndex >= 0) {
            String value = normalized.substring(watchIndex + watchToken.length());
            int ampIndex = value.indexOf('&');
            return ampIndex >= 0 ? value.substring(0, ampIndex) : value;
        }
        int shortIndex = normalized.indexOf("youtu.be/");
        if (shortIndex >= 0) {
            String value = normalized.substring(shortIndex + "youtu.be/".length());
            int queryIndex = value.indexOf('?');
            return queryIndex >= 0 ? value.substring(0, queryIndex) : value;
        }
        return null;
    }

    private String normalizeArtistKey(String artistName) {
        if (artistName == null) {
            return "";
        }
        return artistName.trim().toLowerCase(Locale.ROOT);
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
        int seatCount = soldOut ? SOLD_OUT_SEAT_COUNT : DEFAULT_SEAT_COUNT;
        for (int index = 0; index < DEFAULT_OPTION_COUNT_PER_CONCERT; index++) {
            ConcertOption option = concertOptionRepository.save(
                    new ConcertOption(
                            concert,
                            concertDate.plusDays(index),
                            venue,
                            DEFAULT_TICKET_PRICE_AMOUNT
                    )
            );
            List<Seat> seats = createSeats(option, seatCount);
            if (soldOut) {
                seats.forEach(Seat::reserve);
                seatRepository.saveAll(seats);
            }
        }

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class KpopConcertSeedItem {
        public String artist;
        public String entertainment;
        public String youtubeUrl;
        public Integer seatCount;
        public String saleBucket;
    }

    private enum KpopSaleBucket {
        OPEN,
        OPEN_SOON_5M,
        OPEN_SOON_1H,
        PREOPEN,
        SOLD_OUT,
        UNSCHEDULED;

        static KpopSaleBucket from(String value) {
            String normalized = value == null ? null : value.trim();
            if (normalized == null || normalized.isEmpty()) {
                throw new IllegalArgumentException("saleBucket is required");
            }
            return KpopSaleBucket.valueOf(normalized.toUpperCase(Locale.ROOT));
        }
    }

    private record SeatOccupancy(int reservedCount, int tempReservedCount) {
        int totalOccupied() {
            return Math.max(0, reservedCount) + Math.max(0, tempReservedCount);
        }
    }

    private record ThumbnailPayload(byte[] bytes, String contentType) {
    }
}
