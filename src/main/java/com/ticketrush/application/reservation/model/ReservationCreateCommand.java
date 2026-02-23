package com.ticketrush.application.reservation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreateCommand {
    private Long userId;
    private Long seatId;
    private String requestFingerprint;
    private String deviceFingerprint;

    public ReservationCreateCommand(Long userId, Long seatId) {
        this(userId, seatId, null, null);
    }
}
