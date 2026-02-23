package com.ticketrush.global.push;

import java.util.Set;

public interface WebSocketQueueSubscriptionStore {

    void addQueueSubscriber(Long concertId, Long userId, long expiresAtMillis, long ttlSeconds);

    void removeQueueSubscriber(Long concertId, Long userId);

    Set<String> getConcertIds();

    Set<String> getActiveSubscribers(Long concertId, long nowMillis);
}
