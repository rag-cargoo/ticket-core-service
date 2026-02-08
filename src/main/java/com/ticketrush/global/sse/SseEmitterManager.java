package com.ticketrush.global.sse;

import com.ticketrush.global.config.WaitingQueueProperties;
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
@Component
@RequiredArgsConstructor
public class SseEmitterManager {

    private static final String RESERVATION_KEY_PREFIX = "res:";
    private static final String QUEUE_KEY_PREFIX = "queue:";

    private final WaitingQueueProperties properties;
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
        SseEmitter emitter = new SseEmitter(properties.getSseTimeoutMillis());

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

    public void sendReservationStatus(Long userId, Long seatId, String status) {
        String key = RESERVATION_KEY_PREFIX + userId + ":" + seatId;
        sendAndComplete(key, SseEventNames.RESERVATION_STATUS, status);
    }

    public void sendQueueRankUpdate(Long userId, Long concertId, Object data) {
        String key = toQueueKey(userId, concertId);
        send(key, SseEventNames.RANK_UPDATE, data);
    }

    public void sendQueueActivated(Long userId, Long concertId, Object data) {
        String key = toQueueKey(userId, concertId);
        send(key, SseEventNames.ACTIVE, data);
    }

    public void sendQueueHeartbeat() {
        Object heartbeat = Map.of("timestamp", Instant.now().toString());
        emitters.keySet().stream()
                .filter(key -> key.startsWith(QUEUE_KEY_PREFIX))
                .forEach(key -> send(key, SseEventNames.KEEPALIVE, heartbeat));
    }

    public Set<Long> getSubscribedQueueUsers(Long concertId) {
        String suffix = ":" + concertId;
        return emitters.keySet().stream()
                .filter(key -> key.startsWith(QUEUE_KEY_PREFIX))
                .filter(key -> key.endsWith(suffix))
                .map(this::extractUserIdFromQueueKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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
