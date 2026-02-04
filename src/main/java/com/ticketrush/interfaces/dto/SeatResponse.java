package com.ticketrush.interfaces.dto;

import com.ticketrush.domain.concert.entity.Seat;

public record SeatResponse(
        Long id,
        String seatNumber,
        String status) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getStatus().name());
    }
}
