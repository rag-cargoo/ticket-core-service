package com.ticketrush.api.dto.reservation;

import com.ticketrush.application.reservation.port.inbound.SeatSoftLockUseCase;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatSoftLockReleaseResponse {

    private Long optionId;
    private Long seatId;
    private String status;
    private boolean released;

    public static SeatSoftLockReleaseResponse from(SeatSoftLockUseCase.SeatSoftLockReleaseResult result) {
        return new SeatSoftLockReleaseResponse(
                result.optionId(),
                result.seatId(),
                result.status(),
                result.released()
        );
    }
}
