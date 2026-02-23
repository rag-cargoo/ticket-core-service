package com.ticketrush.global.cache;

import com.ticketrush.application.concert.port.outbound.ConcertReadCacheEvictPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class ConcertReadCacheEvictor implements ConcertReadCacheEvictPort {

    private final CacheManager cacheManager;

    public ConcertReadCacheEvictor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void evictAvailableSeatsByOptionId(Long concertOptionId) {
        if (concertOptionId == null) {
            return;
        }
        Cache cache = cacheManager.getCache(ConcertCacheNames.CONCERT_AVAILABLE_SEATS);
        if (cache == null) {
            return;
        }
        cache.evict(concertOptionId);
    }
}
