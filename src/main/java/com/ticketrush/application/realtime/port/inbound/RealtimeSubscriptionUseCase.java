package com.ticketrush.application.realtime.port.inbound;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface RealtimeSubscriptionUseCase {

    SseEmitter subscribeWaitingQueueSse(Long userId, Long concertId);

    SseEmitter subscribeReservationSse(Long userId, Long seatId);

    String subscribeQueueWebSocket(Long userId, Long concertId);

    void unsubscribeQueueWebSocket(Long userId, Long concertId);

    String subscribeReservationWebSocket(Long userId, Long seatId);

    void unsubscribeReservationWebSocket(Long userId, Long seatId);

    String subscribeSeatMapWebSocket(Long optionId);

    void unsubscribeSeatMapWebSocket(Long optionId);
}
