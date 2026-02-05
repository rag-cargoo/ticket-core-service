package com.ticketrush.domain.reservation.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka를 통해 전달될 예약 요청 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEvent {
    private Long userId;
    private Long seatId;
    private LockType lockType;
    private LocalDateTime eventTime;

    public enum LockType {
        OPTIMISTIC, PESSIMISTIC
    }

    public static ReservationEvent of(Long userId, Long seatId, LockType lockType) {
        return new ReservationEvent(userId, seatId, lockType, LocalDateTime.now());
    }
}
