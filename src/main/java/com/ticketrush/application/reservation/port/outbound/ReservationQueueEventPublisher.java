package com.ticketrush.application.reservation.port.outbound;

import com.ticketrush.application.reservation.model.ReservationQueueLockType;

public interface ReservationQueueEventPublisher {

    void send(Long userId, Long seatId, ReservationQueueLockType lockType);
}
