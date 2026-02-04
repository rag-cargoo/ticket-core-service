package com.ticketrush.interfaces.dto;

import com.ticketrush.domain.reservation.entity.Reservation;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long userId,
        Long seatId,
        LocalDateTime reservationTime) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                reservation.getReservedAt());
    }
}
