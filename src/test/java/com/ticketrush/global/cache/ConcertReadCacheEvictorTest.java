package com.ticketrush.global.cache;

import com.ticketrush.application.port.outbound.ConcertRefreshPushPort;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConcertReadCacheEvictorTest {

    @Test
    void evictAvailableSeatsByOptionId_shouldEvictSeatCacheAndRefreshConcertCards() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                ConcertCacheNames.CONCERT_AVAILABLE_SEATS,
                ConcertCacheNames.CONCERT_LIST,
                ConcertCacheNames.CONCERT_SEARCH
        );
        cacheManager.getCache(ConcertCacheNames.CONCERT_AVAILABLE_SEATS).put(10L, "seat-cache");
        cacheManager.getCache(ConcertCacheNames.CONCERT_LIST).put("k1", "list-cache");
        cacheManager.getCache(ConcertCacheNames.CONCERT_SEARCH).put("k2", "search-cache");
        ConcertRefreshPushPort pushPort = mock(ConcertRefreshPushPort.class);

        ConcertReadCacheEvictor evictor = new ConcertReadCacheEvictor(cacheManager, pushPort);
        evictor.evictAvailableSeatsByOptionId(10L);

        assertThat(cacheManager.getCache(ConcertCacheNames.CONCERT_AVAILABLE_SEATS).get(10L)).isNull();
        assertThat(cacheManager.getCache(ConcertCacheNames.CONCERT_LIST).get("k1")).isNull();
        assertThat(cacheManager.getCache(ConcertCacheNames.CONCERT_SEARCH).get("k2")).isNull();
        verify(pushPort).sendConcertsRefresh(10L);
    }

    @Test
    void evictConcertCards_shouldClearListAndSearchAndBroadcastRefresh() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                ConcertCacheNames.CONCERT_LIST,
                ConcertCacheNames.CONCERT_SEARCH
        );
        cacheManager.getCache(ConcertCacheNames.CONCERT_LIST).put("k1", "list-cache");
        cacheManager.getCache(ConcertCacheNames.CONCERT_SEARCH).put("k2", "search-cache");
        ConcertRefreshPushPort pushPort = mock(ConcertRefreshPushPort.class);

        ConcertReadCacheEvictor evictor = new ConcertReadCacheEvictor(cacheManager, pushPort);
        evictor.evictConcertCards();

        assertThat(cacheManager.getCache(ConcertCacheNames.CONCERT_LIST).get("k1")).isNull();
        assertThat(cacheManager.getCache(ConcertCacheNames.CONCERT_SEARCH).get("k2")).isNull();
        verify(pushPort).sendConcertsRefresh(null);
    }
}
