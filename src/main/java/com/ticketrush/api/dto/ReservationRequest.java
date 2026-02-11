package com.ticketrush.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationRequest {
    private Long userId;
    private Long seatId;
    private String requestFingerprint;
    private String deviceFingerprint;

    public ReservationRequest(Long userId, Long seatId) {
        this(userId, seatId, null, null);
    }

    public ReservationRequest(Long userId, Long seatId, String requestFingerprint, String deviceFingerprint) {
        this.userId = userId;
        this.seatId = seatId;
        this.requestFingerprint = requestFingerprint;
        this.deviceFingerprint = deviceFingerprint;
    }
}
