package com.ticketrush.application.port.outbound;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SsePushPort {

    SseEmitter subscribeReservation(Long userId, Long seatId);

    SseEmitter subscribeQueue(Long userId, Long concertId);

    void sendQueueRankUpdate(Long userId, Long concertId, QueuePushPayload payload);

    void sendQueueActivated(Long userId, Long concertId, QueuePushPayload payload);
}
