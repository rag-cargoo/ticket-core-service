package com.ticketrush.domain.reservation.service;

public interface ReservationQueueService {
    void setStatus(Long userId, Long seatId, String status);

    String getStatus(Long userId, Long seatId);
}
