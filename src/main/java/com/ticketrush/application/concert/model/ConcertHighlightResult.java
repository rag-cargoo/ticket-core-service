package com.ticketrush.application.concert.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConcertHighlightResult {

    private final Long concertId;
    private final String title;
    private final String artistName;
    private final String entertainmentName;
    private final String youtubeVideoUrl;
    private final String thumbnailUrl;
    private final LocalDateTime saleOpensAt;
    private final Long saleOpensInSeconds;
    private final long availableSeatCount;
    private final long totalSeatCount;
    private final int remainingRatioPercent;
}
