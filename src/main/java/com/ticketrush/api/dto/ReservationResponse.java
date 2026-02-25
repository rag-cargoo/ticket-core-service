package com.ticketrush.api.dto;

import com.ticketrush.application.reservation.model.ReservationListItemResult;
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
    private String status;
    private Long concertId;
    private Long optionId;
    private String seatNumber;

    public static ReservationResponse from(ReservationResult result) {
        return new ReservationResponse(
                result.getId(),
                result.getUserId(),
                result.getSeatId(),
                result.getReservationTime(),
                null,
                null,
                null,
                null
        );
    }

    public static ReservationResponse from(ReservationListItemResult result) {
        return new ReservationResponse(
                result.getId(),
                result.getUserId(),
                result.getSeatId(),
                result.getReservationTime(),
                result.getStatus(),
                result.getConcertId(),
                result.getOptionId(),
                result.getSeatNumber()
        );
    }
}
