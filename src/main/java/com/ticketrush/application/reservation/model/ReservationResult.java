package com.ticketrush.application.reservation.model;

import com.ticketrush.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResult {
    private Long id;
    private Long userId;
    private Long seatId;
    private LocalDateTime reservationTime;

    public static ReservationResult from(Reservation reservation) {
        return new ReservationResult(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                reservation.getReservedAt()
        );
    }
}
