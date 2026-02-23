package com.ticketrush.application.port.outbound;

public interface WebSocketSubscriptionPort {

    String subscribeQueue(Long userId, Long concertId);

    void unsubscribeQueue(Long userId, Long concertId);

    String subscribeReservation(Long userId, Long seatId);

    void unsubscribeReservation(Long userId, Long seatId);

    String subscribeSeatMap(Long optionId);

    void unsubscribeSeatMap(Long optionId);
}
