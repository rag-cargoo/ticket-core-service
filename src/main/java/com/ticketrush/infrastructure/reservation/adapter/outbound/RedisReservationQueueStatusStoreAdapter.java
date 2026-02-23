package com.ticketrush.infrastructure.reservation.adapter.outbound;

import com.ticketrush.application.reservation.port.outbound.ReservationQueueStatusStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisReservationQueueStatusStoreAdapter implements ReservationQueueStatusStore {

    private static final String STATUS_KEY_PREFIX = "reservation:status:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void setStatus(Long userId, Long seatId, String status, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key(userId, seatId), status, ttl, unit);
    }

    @Override
    public String getStatus(Long userId, Long seatId) {
        return redisTemplate.opsForValue().get(key(userId, seatId));
    }

    private String key(Long userId, Long seatId) {
        return STATUS_KEY_PREFIX + userId + ":" + seatId;
    }
}
