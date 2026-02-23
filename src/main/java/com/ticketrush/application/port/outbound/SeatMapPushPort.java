package com.ticketrush.application.port.outbound;

public interface SeatMapPushPort {

    default void sendSeatMapStatus(Long optionId, Long seatId, String status, Long ownerUserId, String expiresAt) {
        // Optional capability. Implementations without seat-map channel support can ignore.
    }
}
