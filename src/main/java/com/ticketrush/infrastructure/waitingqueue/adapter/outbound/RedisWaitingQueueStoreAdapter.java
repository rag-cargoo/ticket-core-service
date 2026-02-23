package com.ticketrush.infrastructure.waitingqueue.adapter.outbound;

import com.ticketrush.application.waitingqueue.port.outbound.WaitingQueueStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisWaitingQueueStoreAdapter implements WaitingQueueStore {

    private static final DefaultRedisScript<List> JOIN_AND_RANK_SCRIPT;
    private static final DefaultRedisScript<List> ACTIVATE_USERS_SCRIPT;

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

        ACTIVATE_USERS_SCRIPT = new DefaultRedisScript<>();
        ACTIVATE_USERS_SCRIPT.setResultType(List.class);
        ACTIVATE_USERS_SCRIPT.setScriptText("""
                local queueKey = KEYS[1]
                local activeKeyPrefix = ARGV[1]
                local ttlSeconds = tonumber(ARGV[2])
                local count = tonumber(ARGV[3])

                if count <= 0 then
                    return {}
                end

                local popped = redis.call('ZPOPMIN', queueKey, count)
                local activatedUsers = {}

                for i = 1, #popped, 2 do
                    local userId = popped[i]
                    redis.call('SET', activeKeyPrefix .. userId, 'true', 'EX', ttlSeconds)
                    table.insert(activatedUsers, userId)
                end

                return activatedUsers
                """);
    }

    private final StringRedisTemplate redisTemplate;

    @Override
    public JoinAndRankResult joinAndRank(String queueKey, String activeKey, String userId, long score, long maxQueueSize) {
        List<?> scriptResult = redisTemplate.execute(
                JOIN_AND_RANK_SCRIPT,
                List.of(queueKey, activeKey),
                userId,
                String.valueOf(score),
                String.valueOf(maxQueueSize)
        );
        if (scriptResult == null || scriptResult.size() < 2) {
            return null;
        }
        return new JoinAndRankResult(
                toLong(scriptResult.get(0), 4L),
                toLong(scriptResult.get(1), -1L)
        );
    }

    @Override
    public boolean hasActiveUser(String activeKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(activeKey));
    }

    @Override
    public Long rank(String queueKey, String userId) {
        return redisTemplate.opsForZSet().rank(queueKey, userId);
    }

    @Override
    public List<String> activateUsers(String queueKey, String activeKeyPrefix, long ttlSeconds, long count) {
        List<?> scriptResult = redisTemplate.execute(
                ACTIVATE_USERS_SCRIPT,
                List.of(queueKey),
                activeKeyPrefix,
                String.valueOf(ttlSeconds),
                String.valueOf(count)
        );
        if (scriptResult == null || scriptResult.isEmpty()) {
            return List.of();
        }

        List<String> activatedUsers = new ArrayList<>(scriptResult.size());
        for (Object value : scriptResult) {
            if (value == null) {
                continue;
            }
            activatedUsers.add(String.valueOf(value));
        }
        return activatedUsers;
    }

    @Override
    public Long ttlSeconds(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    private long toLong(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return defaultValue;
    }
}
