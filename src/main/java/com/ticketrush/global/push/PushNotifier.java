package com.ticketrush.global.push;

import java.util.Set;

public interface PushNotifier {

    void sendReservationStatus(Long userId, Long seatId, String status);

    void sendQueueRankUpdate(Long userId, Long concertId, Object data);

    void sendQueueActivated(Long userId, Long concertId, Object data);

    void sendQueueHeartbeat();

    Set<Long> getSubscribedQueueUsers(Long concertId);
}
