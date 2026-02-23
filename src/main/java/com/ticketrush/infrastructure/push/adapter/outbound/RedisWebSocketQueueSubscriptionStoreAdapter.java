package com.ticketrush.infrastructure.push.adapter.outbound;

import com.ticketrush.global.config.WaitingQueueProperties;
import com.ticketrush.global.push.WebSocketQueueSubscriptionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisWebSocketQueueSubscriptionStoreAdapter implements WebSocketQueueSubscriptionStore {

    private final StringRedisTemplate redisTemplate;
    private final WaitingQueueProperties waitingQueueProperties;

    @Override
    public void addQueueSubscriber(Long concertId, Long userId, long expiresAtMillis, long ttlSeconds) {
        String subscriberKey = queueSubscriberKey(concertId);
        redisTemplate.opsForZSet().add(subscriberKey, String.valueOf(userId), expiresAtMillis);
        redisTemplate.opsForSet().add(concertIndexKey(), String.valueOf(concertId));
        redisTemplate.expire(subscriberKey, ttlSeconds * 2, TimeUnit.SECONDS);
        redisTemplate.expire(concertIndexKey(), ttlSeconds * 2, TimeUnit.SECONDS);
    }

    @Override
    public void removeQueueSubscriber(Long concertId, Long userId) {
        String subscriberKey = queueSubscriberKey(concertId);
        redisTemplate.opsForZSet().remove(subscriberKey, String.valueOf(userId));
        Long size = redisTemplate.opsForZSet().zCard(subscriberKey);
        if (size == null || size <= 0) {
            redisTemplate.delete(subscriberKey);
            redisTemplate.opsForSet().remove(concertIndexKey(), String.valueOf(concertId));
        }
    }

    @Override
    public Set<String> getConcertIds() {
        Set<String> concertIds = redisTemplate.opsForSet().members(concertIndexKey());
        return concertIds == null ? Set.of() : concertIds;
    }

    @Override
    public Set<String> getActiveSubscribers(Long concertId, long nowMillis) {
        String subscriberKey = queueSubscriberKey(concertId);
        redisTemplate.opsForZSet().removeRangeByScore(subscriberKey, Double.NEGATIVE_INFINITY, nowMillis - 1);
        Set<String> users = redisTemplate.opsForZSet().rangeByScore(subscriberKey, nowMillis, Double.POSITIVE_INFINITY);
        if (users == null || users.isEmpty()) {
            redisTemplate.opsForSet().remove(concertIndexKey(), String.valueOf(concertId));
            redisTemplate.delete(subscriberKey);
            return Set.of();
        }
        return users;
    }

    private String queueSubscriberKey(Long concertId) {
        return waitingQueueProperties.getWsSubscriberZsetKeyPrefix() + concertId;
    }

    private String concertIndexKey() {
        return waitingQueueProperties.getWsConcertIndexKey();
    }
}
