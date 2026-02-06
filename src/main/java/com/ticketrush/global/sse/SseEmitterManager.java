package com.ticketrush.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 예약 결과 알림 구독
     */
    public SseEmitter subscribeReservation(Long userId, Long seatId) {
        String key = "res:" + userId + ":" + seatId;
        return createEmitter(key, "Connected for Seat: " + seatId);
    }

    /**
     * 대기열 순번 알림 구독
     */
    public SseEmitter subscribeQueue(Long userId, Long concertId) {
        String key = "queue:" + userId + ":" + concertId;
        return createEmitter(key, "Connected for Queue: " + concertId);
    }

    private SseEmitter createEmitter(String key, String initMessage) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 대기열을 고려해 5분으로 연장

        emitter.onCompletion(() -> emitters.remove(key));
        emitter.onTimeout(() -> emitters.remove(key));
        emitter.onError((e) -> emitters.remove(key));

        emitters.put(key, emitter);

        try {
            emitter.send(SseEmitter.event().name("INIT").data(initMessage));
        } catch (IOException e) {
            log.error("SSE Connection Error", e);
        }

        return emitter;
    }

    public void sendReservationStatus(Long userId, Long seatId, String status) {
        String key = "res:" + userId + ":" + seatId;
        sendAndComplete(key, "RESERVATION_STATUS", status);
    }

    public void sendQueueStatus(Long userId, Long concertId, Object data) {
        String key = "queue:" + userId + ":" + concertId;
        send(key, "RANK_UPDATE", data);
    }

    private void sendAndComplete(String key, String eventName, Object data) {
        SseEmitter emitter = emitters.get(key);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                emitter.complete();
            } catch (IOException e) {
                emitters.remove(key);
            }
        }
    }

    private void send(String key, String eventName, Object data) {
        SseEmitter emitter = emitters.get(key);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                emitters.remove(key);
            }
        }
    }
}
