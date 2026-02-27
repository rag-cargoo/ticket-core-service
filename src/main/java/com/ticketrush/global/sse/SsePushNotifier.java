package com.ticketrush.global.sse;

import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.QueueRuntimePushPort;
import com.ticketrush.application.port.outbound.ReservationStatusPushPort;
import com.ticketrush.application.port.outbound.SeatMapPushPort;
import com.ticketrush.application.port.outbound.SsePushPort;
import com.ticketrush.application.port.outbound.ConcertRefreshPushPort;
import com.ticketrush.application.waitingqueue.port.outbound.WaitingQueueConfigPort;
import com.ticketrush.global.monitoring.PushMonitoringMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component("ssePushNotifier")
@RequiredArgsConstructor
public class SsePushNotifier implements QueueRuntimePushPort, ReservationStatusPushPort, SeatMapPushPort, SsePushPort, ConcertRefreshPushPort {

    private static final String RESERVATION_KEY_PREFIX = "res:";
    private static final String QUEUE_KEY_PREFIX = "queue:";

    private final WaitingQueueConfigPort waitingQueueConfig;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 예약 결과 알림 구독
     */
    public SseEmitter subscribeReservation(Long userId, Long seatId) {
        String key = RESERVATION_KEY_PREFIX + userId + ":" + seatId;
        return createEmitter(key, "Connected for Seat: " + seatId);
    }

    /**
     * 대기열 순번 알림 구독
     */
    public SseEmitter subscribeQueue(Long userId, Long concertId) {
        String key = toQueueKey(userId, concertId);
        return createEmitter(key, "Connected for Queue: " + concertId);
    }

    private SseEmitter createEmitter(String key, String initMessage) {
        SseEmitter emitter = new SseEmitter(waitingQueueConfig.getSseTimeoutMillis());

        emitter.onCompletion(() -> removeIfSame(key, emitter));
        emitter.onTimeout(() -> removeIfSame(key, emitter));
        emitter.onError((e) -> removeIfSame(key, emitter));

        SseEmitter previous = emitters.put(key, emitter);
        if (previous != null) {
            try {
                previous.complete();
            } catch (Exception ignored) {
                log.debug("ignore previous emitter completion error, key={}", key);
            }
        }

        try {
            emitter.send(SseEmitter.event().name(SseEventNames.INIT).data(initMessage));
        } catch (IOException e) {
            log.error("SSE Connection Error", e);
        }

        return emitter;
    }

    @Override
    public void sendReservationStatus(Long userId, Long seatId, String status) {
        String key = RESERVATION_KEY_PREFIX + userId + ":" + seatId;
        sendAndComplete(key, SseEventNames.RESERVATION_STATUS, status);
        PushMonitoringMetrics.increment("push", "sse", "reservation_status");
    }

    @Override
    public void sendQueueRankUpdate(Long userId, Long concertId, QueuePushPayload data) {
        String key = toQueueKey(userId, concertId);
        send(key, SseEventNames.RANK_UPDATE, data);
        PushMonitoringMetrics.increment("push", "sse", "queue_rank_update");
    }

    @Override
    public void sendQueueActivated(Long userId, Long concertId, QueuePushPayload data) {
        String key = toQueueKey(userId, concertId);
        send(key, SseEventNames.ACTIVE, data);
        PushMonitoringMetrics.increment("push", "sse", "queue_activated");
    }

    @Override
    public void sendQueueHeartbeat() {
        Object heartbeat = Map.of("timestamp", Instant.now().toString());
        long fanoutCount = 0L;
        for (String key : emitters.keySet()) {
            if (!key.startsWith(QUEUE_KEY_PREFIX)) {
                continue;
            }
            send(key, SseEventNames.KEEPALIVE, heartbeat);
            fanoutCount++;
        }
        if (fanoutCount > 0L) {
            PushMonitoringMetrics.increment("push", "sse", "queue_keepalive", fanoutCount);
            log.info("PUSH_MONITOR transport=sse event=queue_keepalive fanout={}", fanoutCount);
        }
    }

    @Override
    public Set<Long> getSubscribedQueueUsers(Long concertId) {
        String suffix = ":" + concertId;
        return emitters.keySet().stream()
                .filter(key -> key.startsWith(QUEUE_KEY_PREFIX))
                .filter(key -> key.endsWith(suffix))
                .map(this::extractUserIdFromQueueKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public void sendConcertsRefresh(Long optionId) {
        // Service card live refresh is delivered over WebSocket transport.
    }

    private void sendAndComplete(String key, String eventName, Object data) {
        SseEmitter emitter = emitters.get(key);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                emitter.complete();
            } catch (IOException e) {
                log.debug("sendAndComplete failed, key={}", key, e);
                emitters.remove(key, emitter);
            }
        }
    }

    private void send(String key, String eventName, Object data) {
        SseEmitter emitter = emitters.get(key);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.debug("send failed, key={}, event={}", key, eventName, e);
                emitters.remove(key, emitter);
            }
        }
    }

    private String toQueueKey(Long userId, Long concertId) {
        return QUEUE_KEY_PREFIX + userId + ":" + concertId;
    }

    private Long extractUserIdFromQueueKey(String key) {
        String[] parts = key.split(":");
        if (parts.length != 3) {
            return null;
        }
        try {
            return Long.valueOf(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void removeIfSame(String key, SseEmitter emitter) {
        emitters.remove(key, emitter);
    }
}
