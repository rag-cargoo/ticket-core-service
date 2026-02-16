package com.ticketrush.global.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class ConcertReadCacheEvictor {

    private final CacheManager cacheManager;

    public ConcertReadCacheEvictor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

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
