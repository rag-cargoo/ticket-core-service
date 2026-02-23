package com.ticketrush.application.reservation.port.outbound;

import java.util.concurrent.TimeUnit;

public interface ReservationQueueStatusStore {

    void setStatus(Long userId, Long seatId, String status, long ttl, TimeUnit unit);

    String getStatus(Long userId, Long seatId);
}
