package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.ReservationQueueLockType;

public interface ReservationQueueService {
    void setStatus(Long userId, Long seatId, String status);

    String getStatus(Long userId, Long seatId);

    void enqueue(Long userId, Long seatId, ReservationQueueLockType lockType);
}
