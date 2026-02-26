package com.ticketrush.domain.concert.repository;

public interface ConcertSeatStatsProjection {
    Long getConcertId();

    Long getTotalSeatCount();

    Long getAvailableSeatCount();
}
