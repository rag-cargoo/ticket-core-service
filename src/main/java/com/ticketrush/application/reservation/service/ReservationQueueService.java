package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.port.inbound.ReservationQueueOrchestrationUseCase;

public interface ReservationQueueService extends ReservationQueueOrchestrationUseCase {

    void setStatus(Long userId, Long seatId, String status);
}
