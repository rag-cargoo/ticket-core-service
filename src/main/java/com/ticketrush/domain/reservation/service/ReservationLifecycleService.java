package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.reservation.ReservationLifecycleResponse;

public interface ReservationLifecycleService {
    ReservationLifecycleResponse createHold(ReservationRequest request);

    ReservationLifecycleResponse startPaying(Long reservationId, Long userId);

    ReservationLifecycleResponse confirm(Long reservationId, Long userId);

    ReservationLifecycleResponse cancel(Long reservationId, Long userId);

    ReservationLifecycleResponse refund(Long reservationId, Long userId);

    ReservationLifecycleResponse refundAsAdmin(Long reservationId, Long adminUserId);

    ReservationLifecycleResponse getReservation(Long reservationId, Long userId);

    int expireTimedOutHolds();
}
