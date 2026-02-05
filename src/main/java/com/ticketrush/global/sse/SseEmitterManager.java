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

    public SseEmitter subscribe(Long userId, Long seatId) {
        String key = generateKey(userId, seatId);
        SseEmitter emitter = new SseEmitter(60 * 1000L); // 1분 타임아웃

        emitter.onCompletion(() -> emitters.remove(key));
        emitter.onTimeout(() -> emitters.remove(key));
        emitter.onError((e) -> emitters.remove(key));

        emitters.put(key, emitter);

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected for Seat: " + seatId));
        } catch (IOException e) {
            log.error("SSE Connection Error", e);
        }

        return emitter;
    }

    public void send(Long userId, Long seatId, String status) {
        String key = generateKey(userId, seatId);
        SseEmitter emitter = emitters.get(key);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("RESERVATION_STATUS").data(status));
                emitter.complete();
            } catch (IOException e) {
                log.error("SSE Send Error", e);
                emitters.remove(key);
            }
        }
    }

    private String generateKey(Long userId, Long seatId) {
        return userId + ":" + seatId;
    }
}
