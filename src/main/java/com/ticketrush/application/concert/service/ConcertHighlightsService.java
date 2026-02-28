package com.ticketrush.application.concert.service;

import com.ticketrush.application.concert.model.ConcertCardRuntimeSnapshot;
import com.ticketrush.application.concert.model.ConcertHighlightResult;
import com.ticketrush.application.concert.model.ConcertHighlightsResult;
import com.ticketrush.application.concert.model.ConcertResult;
import com.ticketrush.application.concert.port.inbound.ConcertCardRuntimeUseCase;
import com.ticketrush.application.concert.port.inbound.ConcertHighlightsUseCase;
import com.ticketrush.application.concert.port.inbound.ConcertUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConcertHighlightsService implements ConcertHighlightsUseCase {

    private static final int DEFAULT_OPENING_SOON_WITHIN_HOURS = 12;
    private static final int DEFAULT_SELL_OUT_RISK_SEAT_THRESHOLD = 30;
    private static final int DEFAULT_SELL_OUT_RISK_RATIO_THRESHOLD = 18;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 20;
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ConcertUseCase concertUseCase;
    private final ConcertCardRuntimeUseCase concertCardRuntimeUseCase;

    @Override
    public ConcertHighlightsResult getHighlights(Integer openingSoonLimit, Integer sellOutRiskLimit, Instant serverNow) {
        int resolvedOpeningSoonLimit = normalizeLimit(openingSoonLimit);
        int resolvedSellOutRiskLimit = normalizeLimit(sellOutRiskLimit);
        Instant resolvedServerNow = serverNow == null ? Instant.now() : serverNow;
        LocalDate seoulToday = resolvedServerNow.atZone(SEOUL_ZONE_ID).toLocalDate();

        List<ConcertResult> concerts = concertUseCase.getConcertResults();
        List<Long> concertIds = concerts.stream()
                .map(ConcertResult::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, ConcertCardRuntimeSnapshot> runtimeSnapshotMap =
                concertCardRuntimeUseCase.resolveSnapshots(concertIds, resolvedServerNow);

        List<Candidate> candidates = concerts.stream()
                .filter(concert -> concert.getId() != null)
                .map(concert -> new Candidate(
                        concert,
                        runtimeSnapshotMap.getOrDefault(concert.getId(), ConcertCardRuntimeSnapshot.unscheduled())
                ))
                .toList();

        List<ConcertHighlightResult> openingSoon = candidates.stream()
                .filter(candidate -> isOpeningSoonStatus(candidate.runtime().getSaleStatus()))
                .filter(candidate -> candidate.runtime().getSaleOpensInSeconds() != null)
                .filter(candidate -> candidate.runtime().getSaleOpensInSeconds() > 0)
                .filter(candidate -> candidate.runtime().getSaleOpensInSeconds() <= DEFAULT_OPENING_SOON_WITHIN_HOURS * 3600L)
                .filter(candidate -> isSameSeoulDate(candidate.runtime().getSaleOpensAt(), seoulToday))
                .sorted(Comparator
                        .comparingLong((Candidate candidate) -> candidate.runtime().getSaleOpensInSeconds())
                        .thenComparing(candidate -> candidate.runtime().getSaleOpensAt(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(candidate -> candidate.concert().getTitle(), String.CASE_INSENSITIVE_ORDER))
                .limit(resolvedOpeningSoonLimit)
                .map(this::toHighlightResult)
                .toList();

        List<Candidate> sortedBookable = candidates.stream()
                .filter(candidate -> candidate.runtime().isReservationButtonVisible() && candidate.runtime().isReservationButtonEnabled())
                .sorted(Comparator
                        .comparingDouble(this::remainingRatioForSort)
                        .thenComparingLong(candidate -> Math.max(0L, candidate.runtime().getAvailableSeatCount()))
                        .thenComparing(candidate -> candidate.concert().getTitle(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<Candidate> thresholdMatched = sortedBookable.stream()
                .filter(candidate -> isSellOutRiskThresholdMatched(candidate.runtime()))
                .toList();

        List<Candidate> sellOutRiskSource = thresholdMatched.isEmpty() ? sortedBookable : thresholdMatched;
        List<ConcertHighlightResult> sellOutRisk = sellOutRiskSource.stream()
                .limit(resolvedSellOutRiskLimit)
                .map(this::toHighlightResult)
                .toList();

        return ConcertHighlightsResult.builder()
                .openingSoon(openingSoon)
                .sellOutRisk(sellOutRisk)
                .generatedAt(resolvedServerNow.toString())
                .openingSoonWithinHours(DEFAULT_OPENING_SOON_WITHIN_HOURS)
                .sellOutRiskSeatThreshold(DEFAULT_SELL_OUT_RISK_SEAT_THRESHOLD)
                .sellOutRiskRatioThreshold(DEFAULT_SELL_OUT_RISK_RATIO_THRESHOLD)
                .build();
    }

    private int normalizeLimit(Integer rawLimit) {
        if (rawLimit == null) {
            return 3;
        }
        return Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, rawLimit));
    }

    private boolean isOpeningSoonStatus(String saleStatus) {
        if (saleStatus == null) {
            return false;
        }
        return "PREOPEN".equals(saleStatus)
                || "OPEN_SOON_1H".equals(saleStatus)
                || "OPEN_SOON_5M".equals(saleStatus);
    }

    private boolean isSameSeoulDate(LocalDateTime saleOpensAt, LocalDate seoulToday) {
        if (saleOpensAt == null) {
            return false;
        }
        LocalDate saleOpensDateInSeoul = saleOpensAt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(SEOUL_ZONE_ID)
                .toLocalDate();
        return saleOpensDateInSeoul.isEqual(seoulToday);
    }

    private boolean isSellOutRiskThresholdMatched(ConcertCardRuntimeSnapshot runtime) {
        long available = Math.max(0L, runtime.getAvailableSeatCount());
        return available <= DEFAULT_SELL_OUT_RISK_SEAT_THRESHOLD
                || toRemainingRatioPercent(runtime) <= DEFAULT_SELL_OUT_RISK_RATIO_THRESHOLD;
    }

    private double remainingRatioForSort(Candidate candidate) {
        long total = Math.max(1L, candidate.runtime().getTotalSeatCount());
        long available = Math.max(0L, Math.min(candidate.runtime().getAvailableSeatCount(), total));
        return available / (double) total;
    }

    private int toRemainingRatioPercent(ConcertCardRuntimeSnapshot runtime) {
        long total = Math.max(1L, runtime.getTotalSeatCount());
        long available = Math.max(0L, Math.min(runtime.getAvailableSeatCount(), total));
        return (int) Math.round((available * 100.0d) / total);
    }

    private ConcertHighlightResult toHighlightResult(Candidate candidate) {
        ConcertCardRuntimeSnapshot runtime = candidate.runtime();
        return ConcertHighlightResult.builder()
                .concertId(candidate.concert().getId())
                .title(candidate.concert().getTitle())
                .artistName(candidate.concert().getArtistName())
                .entertainmentName(candidate.concert().getEntertainmentName())
                .youtubeVideoUrl(candidate.concert().getYoutubeVideoUrl())
                .thumbnailUrl(candidate.concert().getThumbnailUrl())
                .saleOpensAt(runtime.getSaleOpensAt())
                .saleOpensInSeconds(runtime.getSaleOpensInSeconds())
                .availableSeatCount(Math.max(0L, runtime.getAvailableSeatCount()))
                .totalSeatCount(Math.max(0L, runtime.getTotalSeatCount()))
                .remainingRatioPercent(toRemainingRatioPercent(runtime))
                .build();
    }

    private record Candidate(ConcertResult concert, ConcertCardRuntimeSnapshot runtime) {
    }
}
