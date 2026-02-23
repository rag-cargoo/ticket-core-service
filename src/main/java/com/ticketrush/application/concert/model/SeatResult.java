package com.ticketrush.application.concert.model;

import com.ticketrush.domain.concert.entity.Seat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatResult {
    private Long id;
    private String seatNumber;
    private String status;

    public static SeatResult from(Seat seat) {
        return new SeatResult(seat.getId(), seat.getSeatNumber(), seat.getStatus().name());
    }
}
