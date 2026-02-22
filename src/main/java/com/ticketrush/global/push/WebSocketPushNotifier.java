package com.ticketrush.global.push;

import com.ticketrush.global.config.WaitingQueueProperties;
import com.ticketrush.global.sse.SseEventNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("webSocketPushNotifier")
@RequiredArgsConstructor
public class WebSocketPushNotifier implements PushNotifier {

    private static final String WAITING_QUEUE_TOPIC_PREFIX = "/topic/waiting-queue";
    private static final String RESERVATION_TOPIC_PREFIX = "/topic/reservations";
    private static final String SEAT_MAP_TOPIC_PREFIX = "/topic/seats";

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final WaitingQueueProperties waitingQueueProperties;

    public String subscribeQueue(Long userId, Long concertId) {
        long nowMillis = System.currentTimeMillis();
        long ttlSeconds = normalizedSubscriberTtlSeconds();
        long expiresAtMillis = nowMillis + (ttlSeconds * 1000L);
        String subscriberKey = queueSubscriberKey(concertId);
        redisTemplate.opsForZSet().add(subscriberKey, String.valueOf(userId), expiresAtMillis);
        redisTemplate.opsForSet().add(concertIndexKey(), String.valueOf(concertId));
        redisTemplate.expire(subscriberKey, ttlSeconds * 2, TimeUnit.SECONDS);
        redisTemplate.expire(concertIndexKey(), ttlSeconds * 2, TimeUnit.SECONDS);
        return queueDestination(userId, concertId);
    }

    public void unsubscribeQueue(Long userId, Long concertId) {
        String subscriberKey = queueSubscriberKey(concertId);
        redisTemplate.opsForZSet().remove(subscriberKey, String.valueOf(userId));
        Long size = redisTemplate.opsForZSet().zCard(subscriberKey);
        if (size == null || size <= 0) {
            redisTemplate.delete(subscriberKey);
            redisTemplate.opsForSet().remove(concertIndexKey(), String.valueOf(concertId));
        }
    }

    public String subscribeReservation(Long userId, Long seatId) {
        return reservationDestination(userId, seatId);
    }

    public void unsubscribeReservation(Long userId, Long seatId) {
        // STOMP topic subscription lifecycle is managed by broker/client session.
    }

    public String subscribeSeatMap(Long optionId) {
        return seatMapDestination(optionId);
    }

    public void unsubscribeSeatMap(Long optionId) {
        // STOMP topic subscription lifecycle is managed by broker/client session.
    }

    @Override
    public void sendReservationStatus(Long userId, Long seatId, String status) {
        messagingTemplate.convertAndSend(
                reservationDestination(userId, seatId),
                Map.of(
                        "event", SseEventNames.RESERVATION_STATUS,
                        "userId", userId,
                        "seatId", seatId,
                        "status", status,
                        "timestamp", Instant.now().toString()
                )
        );
    }

    @Override
    public void sendQueueRankUpdate(Long userId, Long concertId, Object data) {
        sendQueueEvent(userId, concertId, SseEventNames.RANK_UPDATE, data);
    }

    @Override
    public void sendQueueActivated(Long userId, Long concertId, Object data) {
        sendQueueEvent(userId, concertId, SseEventNames.ACTIVE, data);
    }

    @Override
    public void sendQueueHeartbeat() {
        String timestamp = Instant.now().toString();
        Set<String> concertIdMembers = redisTemplate.opsForSet().members(concertIndexKey());
        if (concertIdMembers == null || concertIdMembers.isEmpty()) {
            return;
        }
        for (String concertIdMember : concertIdMembers) {
            Long concertId = parseLong(concertIdMember);
            if (concertId == null) {
                continue;
            }
            Set<Long> users = getSubscribedQueueUsers(concertId);
            for (Long userId : users) {
                sendQueueEvent(userId, concertId, SseEventNames.KEEPALIVE, Map.of("timestamp", timestamp));
            }
        }
    }

    @Override
    public Set<Long> getSubscribedQueueUsers(Long concertId) {
        String subscriberKey = queueSubscriberKey(concertId);
        long nowMillis = System.currentTimeMillis();
        redisTemplate.opsForZSet().removeRangeByScore(subscriberKey, Double.NEGATIVE_INFINITY, nowMillis - 1);
        Set<String> users = redisTemplate.opsForZSet().rangeByScore(subscriberKey, nowMillis, Double.POSITIVE_INFINITY);
        if (users == null || users.isEmpty()) {
            redisTemplate.opsForSet().remove(concertIndexKey(), String.valueOf(concertId));
            redisTemplate.delete(subscriberKey);
            return Set.of();
        }
        Set<Long> parsed = new LinkedHashSet<>();
        for (String user : users) {
            Long userId = parseLong(user);
            if (userId != null) {
                parsed.add(userId);
            }
        }
        return parsed;
    }

    @Override
    public void sendSeatMapStatus(Long optionId, Long seatId, String status, Long ownerUserId, String expiresAt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "SEAT_STATUS");
        payload.put("optionId", optionId);
        payload.put("seatId", seatId);
        payload.put("status", status);
        payload.put("transport", "websocket");
        payload.put("timestamp", Instant.now().toString());
        if (ownerUserId != null) {
            payload.put("ownerUserId", ownerUserId);
        }
        if (expiresAt != null) {
            payload.put("expiresAt", expiresAt);
        }
        messagingTemplate.convertAndSend(seatMapDestination(optionId), payload);
    }

    private void sendQueueEvent(Long userId, Long concertId, String eventName, Object data) {
        messagingTemplate.convertAndSend(
                queueDestination(userId, concertId),
                Map.of(
                        "event", eventName,
                        "transport", "websocket",
                        "userId", userId,
                        "concertId", concertId,
                        "data", data
                )
        );
    }

    private String queueDestination(Long userId, Long concertId) {
        return WAITING_QUEUE_TOPIC_PREFIX + "/" + concertId + "/" + userId;
    }

    private String reservationDestination(Long userId, Long seatId) {
        return RESERVATION_TOPIC_PREFIX + "/" + seatId + "/" + userId;
    }

    private String seatMapDestination(Long optionId) {
        return SEAT_MAP_TOPIC_PREFIX + "/" + optionId;
    }

    private String queueSubscriberKey(Long concertId) {
        return waitingQueueProperties.getWsSubscriberZsetKeyPrefix() + concertId;
    }

    private String concertIndexKey() {
        return waitingQueueProperties.getWsConcertIndexKey();
    }

    private long normalizedSubscriberTtlSeconds() {
        long ttlSeconds = waitingQueueProperties.getWsSubscriberTtlSeconds();
        return ttlSeconds > 0 ? ttlSeconds : 300L;
    }

    private Long parseLong(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            log.debug("invalid queue subscriber id: {}", rawValue, exception);
            return null;
        }
    }
}
