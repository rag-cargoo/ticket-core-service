package com.ticketrush.application.reservation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationQueueEvent {
    private Long userId;
    private Long seatId;
    private ReservationQueueLockType lockType;
    private LocalDateTime eventTime;

    public static ReservationQueueEvent of(Long userId, Long seatId, ReservationQueueLockType lockType) {
        return new ReservationQueueEvent(userId, seatId, lockType, LocalDateTime.now());
    }
}
