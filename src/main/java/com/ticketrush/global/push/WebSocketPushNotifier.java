package com.ticketrush.global.push;

import com.ticketrush.application.port.outbound.QueueSubscriberQueryPort;
import com.ticketrush.application.port.outbound.QueueEventName;
import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.QueueRuntimePushPort;
import com.ticketrush.application.port.outbound.WebSocketEventDispatchPort;
import com.ticketrush.application.port.outbound.WebSocketQueueSubscriptionStorePort;
import com.ticketrush.application.port.outbound.WebSocketSubscriptionPort;
import com.ticketrush.application.waitingqueue.port.outbound.WaitingQueueConfigPort;
import com.ticketrush.application.concert.config.ConcertLiveProperties;
import com.ticketrush.global.monitoring.PushMonitoringMetrics;
import com.ticketrush.global.sse.SseEventNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("webSocketPushNotifier")
public class WebSocketPushNotifier implements QueueRuntimePushPort, WebSocketSubscriptionPort, WebSocketEventDispatchPort, QueueSubscriberQueryPort {

    private static final String WAITING_QUEUE_TOPIC_PREFIX = "/topic/waiting-queue";
    private static final String RESERVATION_TOPIC_PREFIX = "/topic/reservations";
    private static final String SEAT_MAP_TOPIC_PREFIX = "/topic/seats";
    private static final String CONCERTS_LIVE_TOPIC = "/topic/concerts/live";

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketQueueSubscriptionStorePort queueSubscriptionStore;
    private final WaitingQueueConfigPort waitingQueueConfig;
    private final ConcertLivePayloadComposer concertLivePayloadComposer;
    private final ConcertLiveProperties concertLiveProperties;

    public WebSocketPushNotifier(
            SimpMessagingTemplate messagingTemplate,
            WebSocketQueueSubscriptionStorePort queueSubscriptionStore,
            WaitingQueueConfigPort waitingQueueConfig,
            ConcertLivePayloadComposer concertLivePayloadComposer,
            ConcertLiveProperties concertLiveProperties
    ) {
        this.messagingTemplate = messagingTemplate;
        this.queueSubscriptionStore = queueSubscriptionStore;
        this.waitingQueueConfig = waitingQueueConfig;
        this.concertLivePayloadComposer = concertLivePayloadComposer;
        this.concertLiveProperties = concertLiveProperties;
    }

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
        PushMonitoringMetrics.increment("push", "websocket", "reservation_status");
    }

    @Override
    public void sendQueueRankUpdate(Long userId, Long concertId, QueuePushPayload data) {
        publishQueueEvent(userId, concertId, QueueEventName.RANK_UPDATE, data);
    }

    @Override
    public void sendQueueActivated(Long userId, Long concertId, QueuePushPayload data) {
        publishQueueEvent(userId, concertId, QueueEventName.ACTIVE, data);
    }

    @Override
    public void sendQueueHeartbeat() {
        String timestamp = Instant.now().toString();
        long fanoutCount = 0L;
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
                publishQueueEvent(
                        userId,
                        concertId,
                        QueueEventName.KEEPALIVE,
                        QueuePushPayload.builder()
                                .userId(userId)
                                .concertId(concertId)
                                .timestamp(timestamp)
                                .build()
                );
                fanoutCount++;
            }
        }
        if (fanoutCount > 0L) {
            log.info("PUSH_MONITOR transport=websocket event=queue_keepalive fanout={}", fanoutCount);
        }
    }

    @Override
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
        PushMonitoringMetrics.increment("push", "websocket", "seat_map_status");
    }

    @Override
    public void publishQueueEvent(Long userId, Long concertId, QueueEventName eventName, QueuePushPayload data) {
        messagingTemplate.convertAndSend(
                queueDestination(userId, concertId),
                Map.of(
                        "event", eventName.name(),
                        "transport", "websocket",
                        "userId", userId,
                        "concertId", concertId,
                        "data", data
                )
        );
        PushMonitoringMetrics.increment("push", "websocket", queueEventMetricName(eventName));
    }

    @Override
    public void sendConcertsRefresh(Long optionId, String timestamp) {
        Instant serverNow = resolveServerNow(timestamp);
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "CONCERTS_REFRESH");
        payload.put("transport", "websocket");
        if (optionId != null) {
            payload.put("optionId", optionId);
        }
        payload.put("timestamp", serverNow.toString());
        payload.put("serverNow", serverNow.toString());
        payload.put("realtimeMode", concertLiveProperties.normalizedMode());
        payload.put("hybridPollIntervalMillis", concertLiveProperties.getHybridPollIntervalMillis());
        payload.put("items", concertLivePayloadComposer.compose(serverNow));
        messagingTemplate.convertAndSend(CONCERTS_LIVE_TOPIC, payload);
        PushMonitoringMetrics.increment("push", "websocket", "concerts_refresh");
    }

    private Instant resolveServerNow(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            return Instant.now();
        }
        try {
            return Instant.parse(timestamp.trim());
        } catch (DateTimeException ignored) {
            return Instant.now();
        }
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
        long ttlSeconds = waitingQueueConfig.getWsSubscriberTtlSeconds();
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

    private String queueEventMetricName(QueueEventName eventName) {
        return switch (eventName) {
            case RANK_UPDATE -> "queue_rank_update";
            case ACTIVE -> "queue_activated";
            case KEEPALIVE -> "queue_keepalive";
        };
    }
}
