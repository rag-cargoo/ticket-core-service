package com.ticketrush.application.reservation.port.inbound;

public interface ReservationQueueRuntimeUseCase extends ReservationQueueOrchestrationUseCase {

    void setStatus(Long userId, Long seatId, String status);
}
