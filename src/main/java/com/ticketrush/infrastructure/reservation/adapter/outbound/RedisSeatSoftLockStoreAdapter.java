package com.ticketrush.infrastructure.reservation.adapter.outbound;

import com.ticketrush.application.reservation.port.outbound.SeatSoftLockStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisSeatSoftLockStoreAdapter implements SeatSoftLockStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean setIfAbsent(String key, String value, long ttl, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, ttl, unit));
    }

    @Override
    public void set(String key, String value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
