package com.ticketrush.domain.concert.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ConcertSaleSnapshot {
    private ConcertSaleStatus saleStatus;
    private LocalDateTime saleOpensAt;
    private Long saleOpensInSeconds;
    private boolean reservationButtonVisible;
    private boolean reservationButtonEnabled;
    private long availableSeatCount;
    private long totalSeatCount;
}
