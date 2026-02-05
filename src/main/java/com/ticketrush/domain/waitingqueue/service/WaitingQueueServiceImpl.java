package com.ticketrush.domain.waitingqueue.service;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class WaitingQueueServiceImpl implements WaitingQueueService {

    private final StringRedisTemplate redisTemplate;

    private static final String QUEUE_KEY_PREFIX = "waiting-queue:";
    private static final String ACTIVE_KEY_PREFIX = "active-user:";

    @Override
    public WaitingQueueResponse join(Long userId, Long concertId) {
        String queueKey = QUEUE_KEY_PREFIX + concertId;
        String userIdStr = String.valueOf(userId);

        // 1. 이미 활성 상태인지 확인
        if (Boolean.TRUE.equals(redisTemplate.hasKey(ACTIVE_KEY_PREFIX + userIdStr))) {
            return WaitingQueueResponse.builder()
                    .userId(userId)
                    .concertId(concertId)
                    .status("ACTIVE")
                    .rank(0L)
                    .builder().build();
        }

        // 2. 대기열에 추가 (점수는 현재 시간)
        redisTemplate.opsForZSet().add(queueKey, userIdStr, System.currentTimeMillis());

        return getStatus(userId, concertId);
    }

    @Override
    public WaitingQueueResponse getStatus(Long userId, Long concertId) {
        String userIdStr = String.valueOf(userId);

        // 1. 활성 상태 확인
        if (Boolean.TRUE.equals(redisTemplate.hasKey(ACTIVE_KEY_PREFIX + userIdStr))) {
            return WaitingQueueResponse.builder()
                    .userId(userId)
                    .concertId(concertId)
                    .status("ACTIVE")
                    .rank(0L)
                    .build();
        }

        // 2. 대기 순번 조회
        String queueKey = QUEUE_KEY_PREFIX + concertId;
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userIdStr);

        return WaitingQueueResponse.builder()
                .userId(userId)
                .concertId(concertId)
                .status(rank != null ? "WAITING" : "NONE")
                .rank(rank != null ? rank + 1 : -1L) // 0-based rank이므로 +1
                .build();
    }

    @Override
    public void activateUsers(Long concertId, long count) {
        String queueKey = QUEUE_KEY_PREFIX + concertId;

        // 상위 N명 추출
        Set<String> users = redisTemplate.opsForZSet().range(queueKey, 0, count - 1);

        if (users != null && !users.isEmpty()) {
            for (String userId : users) {
                // 활성 상태로 전환 (5분간 유효)
                redisTemplate.opsForValue().set(ACTIVE_KEY_PREFIX + userId, "true", java.time.Duration.ofMinutes(5));
                // 대기열에서 제거
                redisTemplate.opsForZSet().remove(queueKey, userId);
            }
        }
    }
}
