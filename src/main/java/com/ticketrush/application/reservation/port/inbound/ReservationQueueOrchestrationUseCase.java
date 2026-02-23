package com.ticketrush.application.reservation.port.inbound;

import com.ticketrush.application.reservation.model.ReservationQueueLockType;

public interface ReservationQueueOrchestrationUseCase {

    void enqueue(Long userId, Long seatId, ReservationQueueLockType lockType);

    String getStatus(Long userId, Long seatId);
}
