package com.ticketrush.application.concert.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConcertCardRuntimeSnapshot {

    private final String saleStatus;
    private final LocalDateTime saleOpensAt;
    private final Long saleOpensInSeconds;
    private final boolean reservationButtonVisible;
    private final boolean reservationButtonEnabled;
    private final long availableSeatCount;
    private final long totalSeatCount;

    public static ConcertCardRuntimeSnapshot unscheduled() {
        return ConcertCardRuntimeSnapshot.builder()
                .saleStatus("UNSCHEDULED")
                .saleOpensAt(null)
                .saleOpensInSeconds(null)
                .reservationButtonVisible(false)
                .reservationButtonEnabled(false)
                .availableSeatCount(0L)
                .totalSeatCount(0L)
                .build();
    }
}
