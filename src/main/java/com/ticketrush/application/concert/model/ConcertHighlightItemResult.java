package com.ticketrush.application.concert.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertHighlightItemResult {
    private Long concertId;
    private String title;
    private String artistName;
    private String entertainmentName;
    private LocalDateTime saleOpensAt;
    private Long saleOpensInSeconds;
    private long availableSeatCount;
    private long totalSeatCount;
    private int remainingRatioPercent;
}
