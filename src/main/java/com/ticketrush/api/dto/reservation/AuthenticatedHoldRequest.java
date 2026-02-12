package com.ticketrush.api.dto.reservation;

import com.ticketrush.api.dto.ReservationRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedHoldRequest {
    private Long seatId;
    private String requestFingerprint;
    private String deviceFingerprint;

    public ReservationRequest toReservationRequest(Long userId) {
        return new ReservationRequest(userId, seatId, requestFingerprint, deviceFingerprint);
    }
}
