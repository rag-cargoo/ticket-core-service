package com.ticketrush.interfaces.dto;

import com.ticketrush.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private Long userId;
    private Long seatId;
    private LocalDateTime reservationTime;

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                reservation.getReservedAt()
        );
    }
}