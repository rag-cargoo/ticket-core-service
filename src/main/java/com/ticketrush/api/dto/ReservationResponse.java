package com.ticketrush.api.dto;

import com.ticketrush.application.reservation.model.ReservationResult;
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

    public static ReservationResponse from(ReservationResult result) {
        return new ReservationResponse(
                result.getId(),
                result.getUserId(),
                result.getSeatId(),
                result.getReservationTime()
        );
    }
}
