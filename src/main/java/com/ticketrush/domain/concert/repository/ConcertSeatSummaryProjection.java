package com.ticketrush.domain.concert.repository;

public interface ConcertSeatSummaryProjection {
    Long getConcertId();

    Long getTotalSeatCount();

    Long getAvailableSeatCount();
}
