package com.ticketrush.application.port.outbound;

public interface WebSocketEventDispatchPort extends ReservationStatusPushPort, SeatMapPushPort {

    void publishQueueEvent(Long userId, Long concertId, QueueEventName eventName, QueuePushPayload data);

    void sendConcertsRefresh(Long optionId, String timestamp);
}
