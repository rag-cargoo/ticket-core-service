package com.ticketrush.interfaces.dto;

public record ReservationRequest(
        Long userId,
        Long seatId) {
}
