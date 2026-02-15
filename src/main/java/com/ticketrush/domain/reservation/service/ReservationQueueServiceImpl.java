package com.ticketrush.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationQueueServiceImpl implements ReservationQueueService {

    private final StringRedisTemplate redisTemplate;

    private static final String STATUS_KEY_PREFIX = "reservation:status:";
    private static final long STATUS_TTL_MINUTES = 30;

    /**
     * 예약 상태 저장 (userId와 seatId 조합을 키로 사용)
     */
    public void setStatus(Long userId, Long seatId, String status) {
        String key = generateKey(userId, seatId);
        redisTemplate.opsForValue().set(key, status, STATUS_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 예약 상태 조회
     */
    public String getStatus(Long userId, Long seatId) {
        return redisTemplate.opsForValue().get(generateKey(userId, seatId));
    }

    private String generateKey(Long userId, Long seatId) {
        return STATUS_KEY_PREFIX + userId + ":" + seatId;
    }
}
