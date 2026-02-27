package com.ticketrush.global.cache;

import com.ticketrush.application.concert.port.outbound.ConcertReadCacheEvictPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class ConcertReadCacheEvictor implements ConcertReadCacheEvictPort {

    private final CacheManager cacheManager;
    private final SimpMessagingTemplate messagingTemplate;

    public ConcertReadCacheEvictor(CacheManager cacheManager, SimpMessagingTemplate messagingTemplate) {
        this.cacheManager = cacheManager;
        this.messagingTemplate = messagingTemplate;
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
        evictAll(ConcertCacheNames.CONCERT_SEARCH);
        evictAll(ConcertCacheNames.CONCERT_LIST);
        messagingTemplate.convertAndSend(
                "/topic/concerts/live",
                Map.of(
                        "event", "CONCERTS_REFRESH",
                        "optionId", concertOptionId,
                        "timestamp", Instant.now().toString()
                )
        );
    }

    private void evictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
