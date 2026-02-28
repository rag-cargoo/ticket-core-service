package com.ticketrush.application.demo.service;

import com.ticketrush.application.concert.port.outbound.ConcertReadCacheEvictPort;
import com.ticketrush.application.demo.config.DemoRebalancerProperties;
import com.ticketrush.application.demo.port.inbound.DemoRebalancerUseCase;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.entity.SalesPolicy;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-rebalancer", name = "enabled", havingValue = "true")
public class DemoRebalancerService implements DemoRebalancerUseCase {

    private static final int MAX_RESERVATIONS_PER_USER = 8;
    private static final Set<String> OPEN_BOOKABLE_PRIORITY = Set.of("bts", "saja boys", "blackpink");
    private static final Set<String> OPEN_SOON_PRIORITY = Set.of(
            "newjeans",
            "le sserafim",
            "tomorrow x together",
            "ive",
            "stayc",
            "red velvet",
            "oh my girl"
    );
    private static final Set<String> PREOPEN_PRIORITY = Set.of("itzy", "twice", "seventeen");
    private static final Set<String> SOLD_OUT_PRIORITY = Set.of(
            "huntrix",
            "bigbang",
            "babymonster",
            "kara",
            "mamamoo",
            "(g)i-dle",
            "nmixx"
    );
    private static final Set<String> UNSCHEDULED_PRIORITY = Set.of("aespa", "kiss of life", "apink");

    private final DemoRebalancerProperties properties;
    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;
    private final SalesPolicyRepository salesPolicyRepository;
    private final ReservationRepository reservationRepository;
    private final ConcertReadCacheEvictPort concertReadCacheEvictPort;
    private final TransactionTemplate transactionTemplate;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "demo-rebalancer-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicReference<DemoRebalanceJobStatus> currentJobRef = new AtomicReference<>(DemoRebalanceJobStatus.idle());

    @Override
    public DemoRebalancerSnapshot getSnapshot() {
        return new DemoRebalancerSnapshot(
                properties.isEnabled(),
                resolveDefaultIntervalMinutes(),
                resolveIntervalOptions(),
                currentJobRef.get()
        );
    }

    @Override
    public DemoRebalanceTriggerResult triggerNow() {
        if (!properties.isEnabled()) {
            return new DemoRebalanceTriggerResult(false, getSnapshot());
        }

        DemoRebalanceJobStatus current = currentJobRef.get();
        if (current.isRunning()) {
            return new DemoRebalanceTriggerResult(false, getSnapshot());
        }

        String jobId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        DemoRebalanceJobStatus next = new DemoRebalanceJobStatus(
                jobId,
                "RUNNING",
                "started",
                2,
                "리밸런서 시작됨",
                now.toString(),
                null,
                now.toString(),
                null
        );
        currentJobRef.set(next);
        executorService.submit(() -> runRebalanceJob(jobId));
        return new DemoRebalanceTriggerResult(true, getSnapshot());
    }

    protected void runRebalanceJob(String jobId) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                updateRunning(jobId, "started", 6, "KPOP20 더미 데이터 검색 중", null);
                List<Concert> targetConcerts = concertRepository.findAllWithOptions().stream()
                        .filter(concert -> {
                            String title = concert.getTitle();
                            return title != null && title.contains("KPOP20_");
                        })
                        .sorted(Comparator.comparing(Concert::getId))
                        .toList();

                if (targetConcerts.isEmpty()) {
                    throw new IllegalStateException("KPOP20 더미 공연을 찾지 못했습니다.");
                }

                List<Long> concertIds = targetConcerts.stream().map(Concert::getId).toList();
                updateRunning(jobId, "cleanup", 14, "기존 예약 데이터 정리 중", "대상 공연 " + targetConcerts.size() + "건");
                reservationRepository.deleteByConcertIds(concertIds);

                int total = targetConcerts.size();
                for (int index = 0; index < total; index++) {
                    Concert concert = targetConcerts.get(index);
                    ConcertOption option = resolvePrimaryOption(concert);
                    if (option == null) {
                        continue;
                    }

                    String artistKey = resolveArtistKey(concert);
                    DemoBucket bucket = resolveBucket(artistKey, index, total);
                    LocalDateTime saleStartAt = resolveSaleStartAt(bucket, artistKey, index);
                    syncSalesPolicy(concert, bucket, saleStartAt);

                    rebalanceSeatStatuses(option.getId(), bucket, artistKey, index);

                    int progress = Math.min(96, 20 + Math.round(((index + 1) * 72f) / Math.max(1, total)));
                    String message = "더미 상태 리밸런싱 중 (" + (index + 1) + "/" + total + ")";
                    updateRunning(jobId, "seeding", progress, message, concert.getTitle());
                    sleepQuietly(120L);
                }

                updateRunning(jobId, "finalizing", 98, "캐시/실시간 푸시 갱신 중", null);
                concertReadCacheEvictPort.evictConcertCards();
            });
            markCompleted(jobId);
        } catch (Exception exception) {
            log.warn("DEMO_REBALANCER execution failed jobId={}", jobId, exception);
            markFailed(jobId, "리밸런싱 실패: " + exception.getMessage());
        }
    }

    private ConcertOption resolvePrimaryOption(Concert concert) {
        if (concert.getOptions() == null || concert.getOptions().isEmpty()) {
            return null;
        }
        return concert.getOptions().stream()
                .min(
                        Comparator.comparing(ConcertOption::getConcertDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(ConcertOption::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                )
                .orElse(null);
    }

    private String resolveArtistKey(Concert concert) {
        String title = concert.getTitle();
        if (title == null) {
            return "";
        }
        String marker = " LIVE IN SEOUL";
        int markerIndex = title.indexOf(marker);
        if (markerIndex > 0) {
            return title.substring(0, markerIndex).trim();
        }
        return title.trim();
    }

    private String normalizeArtistKey(String artistKey) {
        if (artistKey == null) {
            return "";
        }
        return artistKey.trim().toLowerCase(Locale.ROOT);
    }

    private void syncSalesPolicy(Concert concert, DemoBucket bucket, LocalDateTime generalSaleStartAt) {
        SalesPolicy policy = salesPolicyRepository.findByConcertId(concert.getId()).orElse(null);
        if (bucket == DemoBucket.UNSCHEDULED || generalSaleStartAt == null) {
            if (policy != null) {
                salesPolicyRepository.delete(policy);
            }
            return;
        }

        if (policy == null) {
            salesPolicyRepository.save(
                    SalesPolicy.create(
                            concert,
                            null,
                            null,
                            null,
                            generalSaleStartAt,
                            MAX_RESERVATIONS_PER_USER
                    )
            );
            return;
        }
        policy.update(
                null,
                null,
                null,
                generalSaleStartAt,
                Math.max(1, policy.getMaxReservationsPerUser())
        );
    }

    private void rebalanceSeatStatuses(Long optionId, DemoBucket bucket, String artistKey, int indexSeed) {
        List<Long> seatIds = seatRepository.findSeatIdsByConcertOptionId(optionId);
        if (seatIds.isEmpty()) {
            return;
        }
        seatRepository.updateStatusByConcertOptionId(optionId, Seat.SeatStatus.AVAILABLE);

        SeatOccupancy occupancy = resolveSeatOccupancy(bucket, seatIds.size(), artistKey, optionId + indexSeed);
        if (occupancy.totalOccupied() <= 0) {
            return;
        }

        List<Long> shuffled = new ArrayList<>(seatIds);
        Collections.shuffle(shuffled, new java.util.Random(optionId + indexSeed * 31L));

        int reservedCount = Math.min(occupancy.reserved(), shuffled.size());
        int tempReservedCount = Math.min(occupancy.tempReserved(), Math.max(0, shuffled.size() - reservedCount));

        if (reservedCount > 0) {
            List<Long> reservedTargets = new ArrayList<>(shuffled.subList(0, reservedCount));
            seatRepository.updateStatusBySeatIds(reservedTargets, Seat.SeatStatus.RESERVED);
        }
        if (tempReservedCount > 0) {
            int fromIndex = reservedCount;
            int toIndex = reservedCount + tempReservedCount;
            List<Long> tempReservedTargets = new ArrayList<>(shuffled.subList(fromIndex, toIndex));
            seatRepository.updateStatusBySeatIds(tempReservedTargets, Seat.SeatStatus.TEMP_RESERVED);
        }
    }

    private SeatOccupancy resolveSeatOccupancy(DemoBucket bucket, int seatCount, String artistKey, long seed) {
        if (bucket == DemoBucket.SOLD_OUT) {
            return new SeatOccupancy(seatCount, 0);
        }
        if (bucket == DemoBucket.OPEN) {
            String normalizedArtist = normalizeArtistKey(artistKey);
            if ("bts".equals(normalizedArtist)) {
                return occupancyForAvailableRatio(seatCount, 42);
            }
            if ("saja boys".equals(normalizedArtist)) {
                return occupancyForAvailableRatio(seatCount, 48);
            }
            if ("blackpink".equals(normalizedArtist)) {
                return occupancyForAvailableRatio(seatCount, 55);
            }
            int minimumOccupied = Math.max(1, (int) Math.floor(seatCount * 0.28));
            int maximumOccupied = Math.max(minimumOccupied, (int) Math.floor(seatCount * 0.72));
            int occupiedSpread = Math.max(0, maximumOccupied - minimumOccupied);
            int occupied = minimumOccupied + (int) (Math.abs(seed) % (occupiedSpread + 1));
            int tempReserved = Math.max(0, occupied / 5);
            int reserved = Math.max(0, occupied - tempReserved);
            return new SeatOccupancy(reserved, tempReserved);
        }
        return new SeatOccupancy(0, 0);
    }

    private SeatOccupancy occupancyForAvailableRatio(int seatCount, int availableRatioPercent) {
        int safeSeatCount = Math.max(0, seatCount);
        int safeRatio = Math.max(0, Math.min(100, availableRatioPercent));
        int targetAvailable = (int) Math.round(safeSeatCount * (safeRatio / 100.0d));
        int occupied = Math.max(0, safeSeatCount - targetAvailable);
        int tempReserved = Math.max(0, Math.min(occupied, (int) Math.round(occupied * 0.12d)));
        int reserved = Math.max(0, occupied - tempReserved);
        return new SeatOccupancy(reserved, tempReserved);
    }

    private DemoBucket resolveBucket(String artistKey, int index, int total) {
        String normalizedArtist = normalizeArtistKey(artistKey);
        if ("stray kids".equals(normalizedArtist)) {
            return DemoBucket.OPEN_SOON_5M;
        }
        if (OPEN_BOOKABLE_PRIORITY.contains(normalizedArtist)) {
            return DemoBucket.OPEN;
        }
        if (OPEN_SOON_PRIORITY.contains(normalizedArtist)) {
            return DemoBucket.OPEN_SOON_1H;
        }
        if (PREOPEN_PRIORITY.contains(normalizedArtist)) {
            return DemoBucket.PREOPEN;
        }
        if (SOLD_OUT_PRIORITY.contains(normalizedArtist)) {
            return DemoBucket.SOLD_OUT;
        }
        if (UNSCHEDULED_PRIORITY.contains(normalizedArtist)) {
            return DemoBucket.UNSCHEDULED;
        }

        if (index == 0) {
            return DemoBucket.OPEN_SOON_5M;
        }
        int openSoonUpperBound = Math.min(total - 1, 4);
        if (index > 0 && index <= openSoonUpperBound) {
            return DemoBucket.OPEN_SOON_1H;
        }
        int soldOutIndex = Math.max(1, total / 2);
        if (index == soldOutIndex) {
            return DemoBucket.SOLD_OUT;
        }
        int preopenStart = Math.max(1, total - 2);
        if (index >= preopenStart) {
            return DemoBucket.PREOPEN;
        }
        return DemoBucket.OPEN;
    }

    private LocalDateTime resolveSaleStartAt(DemoBucket bucket, String artistKey, int indexSeed) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
        String normalizedArtist = normalizeArtistKey(artistKey);
        return switch (bucket) {
            case OPEN -> now.minusMinutes(resolveOpenOffsetMinutes(normalizedArtist, indexSeed));
            case SOLD_OUT -> now.minusHours(2L).minusMinutes(indexSeed % 45L);
            case OPEN_SOON_5M -> now.plusMinutes(3L).plusSeconds((indexSeed * 7L) % 45L);
            case OPEN_SOON_1H -> now.plusMinutes(resolveOpeningSoonOffsetMinutes(normalizedArtist, indexSeed));
            case PREOPEN -> now.plusHours(4L + (indexSeed % 3L)).plusMinutes((indexSeed * 13L) % 60L);
            case UNSCHEDULED -> null;
        };
    }

    private long resolveOpenOffsetMinutes(String normalizedArtist, int indexSeed) {
        return switch (normalizedArtist) {
            case "bts" -> 78L;
            case "saja boys" -> 66L;
            case "blackpink" -> 54L;
            default -> 30L + (indexSeed % 24L);
        };
    }

    private long resolveOpeningSoonOffsetMinutes(String normalizedArtist, int indexSeed) {
        return switch (normalizedArtist) {
            case "newjeans" -> 13L;
            case "le sserafim" -> 20L;
            case "tomorrow x together" -> 27L;
            case "ive" -> 34L;
            case "stayc" -> 41L;
            case "red velvet" -> 48L;
            case "oh my girl" -> 55L;
            default -> 12L + ((indexSeed * 9L) % 44L);
        };
    }

    private void updateRunning(
            String jobId,
            String phase,
            Integer progressPercent,
            String message,
            String lastLogLine
    ) {
        currentJobRef.updateAndGet(current -> {
            if (!current.isRunning() || !current.jobId().equals(jobId)) {
                return current;
            }
            String nextPhase = phase == null ? current.phase() : phase;
            int nextProgress = progressPercent == null ? current.progressPercent() : Math.max(current.progressPercent(), progressPercent);
            String nextMessage = message == null ? current.message() : message;
            String now = Instant.now().toString();
            return new DemoRebalanceJobStatus(
                    current.jobId(),
                    current.status(),
                    nextPhase,
                    Math.min(99, Math.max(0, nextProgress)),
                    nextMessage,
                    current.startedAt(),
                    current.finishedAt(),
                    now,
                    lastLogLine == null ? current.lastLogLine() : truncateLine(lastLogLine)
            );
        });
    }

    private void markCompleted(String jobId) {
        currentJobRef.updateAndGet(current -> {
            if (!current.jobId().equals(jobId)) {
                return current;
            }
            String now = Instant.now().toString();
            return new DemoRebalanceJobStatus(
                    current.jobId(),
                    "COMPLETED",
                    "completed",
                    100,
                    "리밸런서 완료",
                    current.startedAt(),
                    now,
                    now,
                    current.lastLogLine()
            );
        });
    }

    private void markFailed(String jobId, String message) {
        currentJobRef.updateAndGet(current -> {
            if (!current.jobId().equals(jobId)) {
                return current;
            }
            String now = Instant.now().toString();
            return new DemoRebalanceJobStatus(
                    current.jobId(),
                    "FAILED",
                    "failed",
                    Math.max(0, Math.min(99, current.progressPercent())),
                    message,
                    current.startedAt(),
                    now,
                    now,
                    current.lastLogLine()
            );
        });
    }

    private String truncateLine(String rawLine) {
        if (rawLine == null) {
            return null;
        }
        String trimmed = rawLine.trim();
        if (trimmed.length() <= 280) {
            return trimmed;
        }
        return trimmed.substring(0, 280);
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Integer> resolveIntervalOptions() {
        List<Integer> options = properties.getIntervalOptionsMinutes();
        if (options == null || options.isEmpty()) {
            return List.of(0, 10, 30, 60);
        }
        return options.stream()
                .filter(value -> value != null && value >= 0 && value <= 24 * 60)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private int resolveDefaultIntervalMinutes() {
        int configured = properties.getDefaultIntervalMinutes();
        List<Integer> options = resolveIntervalOptions();
        if (configured >= 0 && options.contains(configured)) {
            return configured;
        }
        if (options.contains(60)) {
            return 60;
        }
        return options.stream().filter(value -> value > 0).findFirst().orElse(0);
    }

    @PreDestroy
    void shutdownExecutor() {
        executorService.shutdownNow();
    }

    private enum DemoBucket {
        OPEN,
        OPEN_SOON_1H,
        OPEN_SOON_5M,
        PREOPEN,
        SOLD_OUT,
        UNSCHEDULED
    }

    private record SeatOccupancy(int reserved, int tempReserved) {
        int totalOccupied() {
            return reserved + tempReserved;
        }
    }

}
