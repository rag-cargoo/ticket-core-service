package com.ticketrush.api.dto;

import com.ticketrush.application.concert.model.ConcertHighlightItemResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertHighlightResponse {
    private Long concertId;
    private String title;
    private String artistName;
    private String entertainmentName;
    private LocalDateTime saleOpensAt;
    private Long saleOpensInSeconds;
    private long availableSeatCount;
    private long totalSeatCount;
    private int remainingRatioPercent;

    public static ConcertHighlightResponse from(ConcertHighlightItemResult result) {
        return new ConcertHighlightResponse(
                result.getConcertId(),
                result.getTitle(),
                result.getArtistName(),
                result.getEntertainmentName(),
                result.getSaleOpensAt(),
                result.getSaleOpensInSeconds(),
                result.getAvailableSeatCount(),
                result.getTotalSeatCount(),
                result.getRemainingRatioPercent()
        );
    }
}
