package com.ticketrush.domain.waitingqueue.service;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WaitingQueueServiceImpl implements WaitingQueueService {

    private final StringRedisTemplate redisTemplate;
    private final com.ticketrush.global.config.WaitingQueueProperties properties;

    @Override
    public WaitingQueueResponse join(Long userId, Long concertId) {
        String queueKey = properties.getQueueKeyPrefix() + concertId;
        String userIdStr = String.valueOf(userId);

        // 1. 이미 활성 상태인지 확인
        if (Boolean.TRUE.equals(redisTemplate.hasKey(properties.getActiveKeyPrefix() + userIdStr))) {
            return WaitingQueueResponse.builder()
                    .userId(userId)
                    .concertId(concertId)
                    .status(WaitingQueueStatus.ACTIVE.name())
                    .rank(0L)
                    .build();
        }

        // 2. Throttling 체크 (입구 컷)
        Long currentQueueSize = redisTemplate.opsForZSet().size(queueKey);
        if (currentQueueSize != null && currentQueueSize >= properties.getMaxQueueSize()) {
            return WaitingQueueResponse.builder()
                    .userId(userId)
                    .concertId(concertId)
                    .status(WaitingQueueStatus.REJECTED.name())
                    .rank(-1L)
                    .build();
        }

        // 3. 대기열에 추가
        redisTemplate.opsForZSet().add(queueKey, userIdStr, System.currentTimeMillis());

        return getStatus(userId, concertId);
    }

    @Override
    public WaitingQueueResponse getStatus(Long userId, Long concertId) {
        String userIdStr = String.valueOf(userId);

        // 1. 활성 상태 확인
        if (Boolean.TRUE.equals(redisTemplate.hasKey(properties.getActiveKeyPrefix() + userIdStr))) {
            return WaitingQueueResponse.builder()
                    .userId(userId)
                    .concertId(concertId)
                    .status(WaitingQueueStatus.ACTIVE.name())
                    .rank(0L)
                    .build();
        }

        // 2. 대기 순번 조회
        String queueKey = properties.getQueueKeyPrefix() + concertId;
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userIdStr);

        return WaitingQueueResponse.builder()
                .userId(userId)
                .concertId(concertId)
                .status(rank != null ? WaitingQueueStatus.WAITING.name() : WaitingQueueStatus.NONE.name())
                .rank(rank != null ? rank + 1 : -1L) // 0-based rank이므로 +1
                .build();
    }

    @Override
    public List<Long> activateUsers(Long concertId, long count) {
        String queueKey = properties.getQueueKeyPrefix() + concertId;
        List<Long> activatedUsers = new ArrayList<>();

        // 상위 N명 추출
        Set<String> users = redisTemplate.opsForZSet().range(queueKey, 0, count - 1);

        if (users != null && !users.isEmpty()) {
            for (String userId : users) {
                // 활성 상태로 전환
                redisTemplate.opsForValue().set(
                        properties.getActiveKeyPrefix() + userId,
                        "true",
                        java.time.Duration.ofMinutes(properties.getActiveTtlMinutes())
                );
                // 대기열에서 제거
                redisTemplate.opsForZSet().remove(queueKey, userId);
                activatedUsers.add(Long.valueOf(userId));
            }
        }

        return activatedUsers;
    }

    @Override
    public Long getActiveTtlSeconds(Long userId) {
        String activeKey = properties.getActiveKeyPrefix() + userId;
        Long ttl = redisTemplate.getExpire(activeKey, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? ttl : 0L;
    }
}
