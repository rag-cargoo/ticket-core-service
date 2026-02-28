package com.ticketrush.global.cache;

import com.ticketrush.application.concert.port.outbound.ConcertReadCacheEvictPort;
import com.ticketrush.application.port.outbound.ConcertRefreshPushPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConcertReadCacheEvictor implements ConcertReadCacheEvictPort {

    private final CacheManager cacheManager;
    private final ConcertRefreshPushPort concertRefreshPushPort;

    public ConcertReadCacheEvictor(CacheManager cacheManager, ConcertRefreshPushPort concertRefreshPushPort) {
        this.cacheManager = cacheManager;
        this.concertRefreshPushPort = concertRefreshPushPort;
    }

    @Override
    public void evictAvailableSeatsByOptionId(Long concertOptionId) {
        if (concertOptionId == null) {
            return;
        }
        Cache cache = cacheManager.getCache(ConcertCacheNames.CONCERT_AVAILABLE_SEATS);
        if (cache != null) {
            cache.evict(concertOptionId);
        }
        evictConcertCardsInternal(concertOptionId);
    }

    @Override
    public void evictConcertCards() {
        evictConcertCardsInternal(null);
    }

    private void evictConcertCardsInternal(Long optionId) {
        clearCache(ConcertCacheNames.CONCERT_SEARCH);
        clearCache(ConcertCacheNames.CONCERT_LIST);
        try {
            concertRefreshPushPort.sendConcertsRefresh(optionId);
        } catch (RuntimeException exception) {
            log.warn("Failed to publish concert refresh push event. optionId={}", optionId, exception);
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
