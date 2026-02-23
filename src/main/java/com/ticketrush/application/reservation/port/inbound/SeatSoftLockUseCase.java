package com.ticketrush.application.reservation.port.inbound;

import java.time.LocalDateTime;

public interface SeatSoftLockUseCase {

    SeatSoftLockAcquireResult acquire(Long userId, Long seatId, String requestId);

    SeatSoftLockReleaseResult release(Long userId, Long seatId);

    void ensureHoldableByUser(Long userId, Long seatId);

    void promoteToHold(Long userId, Long seatId, LocalDateTime holdExpiresAt);

    record SeatSoftLockAcquireResult(
            Long optionId,
            Long seatId,
            Long ownerUserId,
            String status,
            String requestId,
            String expiresAt,
            Long ttlSeconds
    ) {
    }

    record SeatSoftLockReleaseResult(
            Long optionId,
            Long seatId,
            String status,
            boolean released
    ) {
    }
}
