package com.ticketrush.application.reservation.model;

import com.ticketrush.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationListItemResult {
    private Long id;
    private Long userId;
    private Long seatId;
    private LocalDateTime reservationTime;
    private String status;
    private Long concertId;
    private Long optionId;
    private String seatNumber;

    public static ReservationListItemResult from(Reservation reservation) {
        return new ReservationListItemResult(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                reservation.getReservedAt(),
                reservation.getStatus().name(),
                reservation.getSeat().getConcertOption().getConcert().getId(),
                reservation.getSeat().getConcertOption().getId(),
                reservation.getSeat().getSeatNumber()
        );
    }
}
