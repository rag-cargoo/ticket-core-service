package com.ticketrush.domain.waitingqueue.service;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    private static final long JOIN_RESULT_ACTIVE = 1L;
    private static final long JOIN_RESULT_REJECTED = 2L;
    private static final long JOIN_RESULT_WAITING = 3L;
    private static final long JOIN_RESULT_UNKNOWN = 4L;
    private static final DefaultRedisScript<List> JOIN_AND_RANK_SCRIPT;

    static {
        JOIN_AND_RANK_SCRIPT = new DefaultRedisScript<>();
        JOIN_AND_RANK_SCRIPT.setResultType(List.class);
        JOIN_AND_RANK_SCRIPT.setScriptText("""
                local queueKey = KEYS[1]
                local activeKey = KEYS[2]
                local userId = ARGV[1]
                local score = tonumber(ARGV[2])
                local maxQueueSize = tonumber(ARGV[3])

                if redis.call('EXISTS', activeKey) == 1 then
                    return {1, 0}
                end

                local existingRank = redis.call('ZRANK', queueKey, userId)
                if existingRank then
                    return {3, existingRank + 1}
                end

                local currentQueueSize = redis.call('ZCARD', queueKey)
                if currentQueueSize >= maxQueueSize then
                    return {2, -1}
                end

                redis.call('ZADD', queueKey, score, userId)
                local addedRank = redis.call('ZRANK', queueKey, userId)
                if addedRank then
                    return {3, addedRank + 1}
                end

                return {4, -1}
                """);
    }

    @Override
    public WaitingQueueResponse join(Long userId, Long concertId) {
        String queueKey = properties.getQueueKeyPrefix() + concertId;
        String userIdStr = String.valueOf(userId);
        String activeKey = properties.getActiveKeyPrefix() + userIdStr;

        // Redis round trip을 줄이기 위해 join/throttling/rank 계산을 Lua 스크립트로 원자 처리한다.
        List<?> scriptResult = redisTemplate.execute(
                JOIN_AND_RANK_SCRIPT,
                List.of(queueKey, activeKey),
                userIdStr,
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(properties.getMaxQueueSize())
        );
        if (scriptResult == null || scriptResult.size() < 2) {
            return getStatus(userId, concertId);
        }

        long resultCode = toLong(scriptResult.get(0), JOIN_RESULT_UNKNOWN);
        long rank = toLong(scriptResult.get(1), -1L);

        if (resultCode == JOIN_RESULT_ACTIVE) {
            return buildResponse(userId, concertId, WaitingQueueStatus.ACTIVE.name(), 0L);
        }
        if (resultCode == JOIN_RESULT_REJECTED) {
            return buildResponse(userId, concertId, WaitingQueueStatus.REJECTED.name(), -1L);
        }
        if (resultCode == JOIN_RESULT_WAITING) {
            return buildResponse(userId, concertId, WaitingQueueStatus.WAITING.name(), rank > 0 ? rank : 1L);
        }

        return getStatus(userId, concertId);
    }

    @Override
    public WaitingQueueResponse getStatus(Long userId, Long concertId) {
        String userIdStr = String.valueOf(userId);

        // 1. 활성 상태 확인
        if (Boolean.TRUE.equals(redisTemplate.hasKey(properties.getActiveKeyPrefix() + userIdStr))) {
            return buildResponse(userId, concertId, WaitingQueueStatus.ACTIVE.name(), 0L);
        }

        // 2. 대기 순번 조회
        String queueKey = properties.getQueueKeyPrefix() + concertId;
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userIdStr);

        return buildResponse(
                userId,
                concertId,
                rank != null ? WaitingQueueStatus.WAITING.name() : WaitingQueueStatus.NONE.name(),
                rank != null ? rank + 1 : -1L // 0-based rank이므로 +1
        );
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

    private WaitingQueueResponse buildResponse(Long userId, Long concertId, String status, Long rank) {
        return WaitingQueueResponse.builder()
                .userId(userId)
                .concertId(concertId)
                .status(status)
                .rank(rank)
                .build();
    }

    private long toLong(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return defaultValue;
    }
}
