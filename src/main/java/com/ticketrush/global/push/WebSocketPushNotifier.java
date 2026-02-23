package com.ticketrush.global.push;

import com.ticketrush.global.config.WaitingQueueProperties;
import com.ticketrush.global.sse.SseEventNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("webSocketPushNotifier")
@RequiredArgsConstructor
public class WebSocketPushNotifier implements PushNotifier {

    private static final String WAITING_QUEUE_TOPIC_PREFIX = "/topic/waiting-queue";
    private static final String RESERVATION_TOPIC_PREFIX = "/topic/reservations";
    private static final String SEAT_MAP_TOPIC_PREFIX = "/topic/seats";

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketQueueSubscriptionStore queueSubscriptionStore;
    private final WaitingQueueProperties waitingQueueProperties;

    public String subscribeQueue(Long userId, Long concertId) {
        long nowMillis = System.currentTimeMillis();
        long ttlSeconds = normalizedSubscriberTtlSeconds();
        long expiresAtMillis = nowMillis + (ttlSeconds * 1000L);
        queueSubscriptionStore.addQueueSubscriber(concertId, userId, expiresAtMillis, ttlSeconds);
        return queueDestination(userId, concertId);
    }

    public void unsubscribeQueue(Long userId, Long concertId) {
        queueSubscriptionStore.removeQueueSubscriber(concertId, userId);
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
        publishQueueEvent(userId, concertId, SseEventNames.RANK_UPDATE, data);
    }

    @Override
    public void sendQueueActivated(Long userId, Long concertId, Object data) {
        publishQueueEvent(userId, concertId, SseEventNames.ACTIVE, data);
    }

    @Override
    public void sendQueueHeartbeat() {
        String timestamp = Instant.now().toString();
        Set<String> concertIdMembers = queueSubscriptionStore.getConcertIds();
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
                publishQueueEvent(userId, concertId, SseEventNames.KEEPALIVE, Map.of("timestamp", timestamp));
            }
        }
    }

    public Map<Long, Set<Long>> snapshotQueueSubscribers() {
        Set<String> concertIdMembers = queueSubscriptionStore.getConcertIds();
        if (concertIdMembers == null || concertIdMembers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Set<Long>> snapshot = new HashMap<>();
        for (String concertIdMember : concertIdMembers) {
            Long concertId = parseLong(concertIdMember);
            if (concertId == null) {
                continue;
            }
            Set<Long> users = getSubscribedQueueUsers(concertId);
            if (!users.isEmpty()) {
                snapshot.put(concertId, users);
            }
        }
        return snapshot;
    }

    @Override
    public Set<Long> getSubscribedQueueUsers(Long concertId) {
        long nowMillis = System.currentTimeMillis();
        Set<String> users = queueSubscriptionStore.getActiveSubscribers(concertId, nowMillis);
        if (users == null || users.isEmpty()) {
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

    public void publishQueueEvent(Long userId, Long concertId, String eventName, Object data) {
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
