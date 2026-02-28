package com.ticketrush.application.concert.service;

import com.ticketrush.application.concert.model.ConcertCardRuntimeSnapshot;
import com.ticketrush.application.concert.port.inbound.ConcertCardRuntimeUseCase;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.ConcertSeatSummaryProjection;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.entity.SalesPolicy;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConcertCardRuntimeService implements ConcertCardRuntimeUseCase {

    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;
    private final SalesPolicyRepository salesPolicyRepository;

    @Override
    public List<Long> findAllConcertIds() {
        return concertRepository.findAllIds();
    }

    @Override
    public Map<Long, ConcertCardRuntimeSnapshot> resolveSnapshots(List<Long> concertIds, Instant serverNow) {
        if (concertIds == null || concertIds.isEmpty()) {
            return Map.of();
        }

        LocalDateTime referenceTime = LocalDateTime.ofInstant(
                serverNow == null ? Instant.now() : serverNow,
                ZoneId.systemDefault()
        );

        Map<Long, SeatSummary> seatSummaryMap = toSeatSummaryMap(
                seatRepository.summarizeSeatCountsByConcertIds(concertIds, Seat.SeatStatus.AVAILABLE)
        );
        Map<Long, SalesPolicy> policyMap = toPolicyMap(salesPolicyRepository.findByConcertIdIn(concertIds));

        Map<Long, ConcertCardRuntimeSnapshot> snapshots = new HashMap<>();
        for (Long concertId : concertIds) {
            if (concertId == null) {
                continue;
            }
            SeatSummary seatSummary = seatSummaryMap.getOrDefault(concertId, SeatSummary.empty());
            SalesPolicy policy = policyMap.get(concertId);
            snapshots.put(concertId, buildSnapshot(policy, seatSummary, referenceTime));
        }
        return snapshots;
    }

    private Map<Long, SeatSummary> toSeatSummaryMap(List<ConcertSeatSummaryProjection> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, SeatSummary> result = new HashMap<>();
        for (ConcertSeatSummaryProjection row : rows) {
            if (row == null || row.getConcertId() == null) {
                continue;
            }
            long total = row.getTotalSeatCount() == null ? 0L : Math.max(0L, row.getTotalSeatCount());
            long available = row.getAvailableSeatCount() == null ? 0L : Math.max(0L, row.getAvailableSeatCount());
            result.put(row.getConcertId(), new SeatSummary(total, Math.min(available, total)));
        }
        return result;
    }

    private Map<Long, SalesPolicy> toPolicyMap(List<SalesPolicy> policies) {
        if (policies == null || policies.isEmpty()) {
            return Map.of();
        }
        Map<Long, SalesPolicy> result = new HashMap<>();
        for (SalesPolicy policy : policies) {
            if (policy == null || policy.getConcert() == null || policy.getConcert().getId() == null) {
                continue;
            }
            result.put(policy.getConcert().getId(), policy);
        }
        return result;
    }

    private ConcertCardRuntimeSnapshot buildSnapshot(
            SalesPolicy policy,
            SeatSummary seatSummary,
            LocalDateTime referenceTime
    ) {
        if (policy == null || policy.getGeneralSaleStartAt() == null) {
            return ConcertCardRuntimeSnapshot.builder()
                    .saleStatus("UNSCHEDULED")
                    .saleOpensAt(null)
                    .saleOpensInSeconds(null)
                    .reservationButtonVisible(false)
                    .reservationButtonEnabled(false)
                    .availableSeatCount(seatSummary.availableSeatCount())
                    .totalSeatCount(seatSummary.totalSeatCount())
                    .build();
        }

        LocalDateTime openAt = policy.getGeneralSaleStartAt();
        if (referenceTime.isBefore(openAt)) {
            long seconds = Math.max(0L, Duration.between(referenceTime, openAt).getSeconds());
            String saleStatus = seconds <= 300L ? "OPEN_SOON_5M"
                    : seconds <= 3600L ? "OPEN_SOON_1H"
                    : "PREOPEN";
            return ConcertCardRuntimeSnapshot.builder()
                    .saleStatus(saleStatus)
                    .saleOpensAt(openAt)
                    .saleOpensInSeconds(seconds)
                    .reservationButtonVisible(false)
                    .reservationButtonEnabled(false)
                    .availableSeatCount(seatSummary.availableSeatCount())
                    .totalSeatCount(seatSummary.totalSeatCount())
                    .build();
        }

        boolean soldOut = seatSummary.totalSeatCount() > 0L && seatSummary.availableSeatCount() <= 0L;
        String saleStatus = soldOut ? "SOLD_OUT" : "OPEN";
        boolean reservationEnabled = !soldOut && seatSummary.availableSeatCount() > 0L;
        boolean reservationVisible = soldOut || reservationEnabled;
        return ConcertCardRuntimeSnapshot.builder()
                .saleStatus(saleStatus)
                .saleOpensAt(openAt)
                .saleOpensInSeconds(0L)
                .reservationButtonVisible(reservationVisible)
                .reservationButtonEnabled(reservationEnabled)
                .availableSeatCount(seatSummary.availableSeatCount())
                .totalSeatCount(seatSummary.totalSeatCount())
                .build();
    }

    private record SeatSummary(long totalSeatCount, long availableSeatCount) {
        static SeatSummary empty() {
            return new SeatSummary(0L, 0L);
        }
    }
}
