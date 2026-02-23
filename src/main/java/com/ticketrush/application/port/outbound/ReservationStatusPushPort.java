package com.ticketrush.application.port.outbound;

public interface ReservationStatusPushPort {

    void sendReservationStatus(Long userId, Long seatId, String status);
}
