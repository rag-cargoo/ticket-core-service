package com.ticketrush.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.ticketrush.global.cache.ConcertCacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ConcertCacheConfig {

    private final ConcertCacheProperties properties;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                createCache(ConcertCacheNames.CONCERT_LIST, properties.getListTtl(), properties.getListMaxSize()),
                createCache(ConcertCacheNames.CONCERT_SEARCH, properties.getSearchTtl(), properties.getSearchMaxSize()),
                createCache(ConcertCacheNames.CONCERT_OPTIONS, properties.getOptionsTtl(), properties.getOptionsMaxSize()),
                createCache(ConcertCacheNames.CONCERT_AVAILABLE_SEATS, properties.getAvailableSeatsTtl(), properties.getAvailableSeatsMaxSize())
        ));
        return manager;
    }

    private CaffeineCache createCache(String name, Duration ttl, long maxSize) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttl)
                        .maximumSize(maxSize)
                        .build()
        );
    }
}
