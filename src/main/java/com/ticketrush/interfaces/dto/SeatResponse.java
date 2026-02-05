package com.ticketrush.interfaces.dto;

import com.ticketrush.domain.concert.entity.Seat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private String status;

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getStatus().name());
    }
}