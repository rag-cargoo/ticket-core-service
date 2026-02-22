package com.ticketrush.api.dto.reservation;

import com.ticketrush.domain.reservation.service.SeatSoftLockService;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatSoftLockAcquireResponse {

    private Long optionId;
    private Long seatId;
    private Long ownerUserId;
    private String status;
    private String requestId;
    private String expiresAt;
    private Long ttlSeconds;

    public static SeatSoftLockAcquireResponse from(SeatSoftLockService.SeatSoftLockAcquireResult result) {
        return new SeatSoftLockAcquireResponse(
                result.optionId(),
                result.seatId(),
                result.ownerUserId(),
                result.status(),
                result.requestId(),
                result.expiresAt(),
                result.ttlSeconds()
        );
    }
}
