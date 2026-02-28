package com.ticketrush.application.concert.port.outbound;

public interface ConcertReadCacheEvictPort {

    void evictAvailableSeatsByOptionId(Long concertOptionId);

    void evictConcertCards();
}
