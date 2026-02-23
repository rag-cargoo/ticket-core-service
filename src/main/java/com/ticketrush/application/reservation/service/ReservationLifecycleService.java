package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationLifecycleResult;

public interface ReservationLifecycleService {
    ReservationLifecycleResult createHold(ReservationCreateCommand command);

    ReservationLifecycleResult startPaying(Long reservationId, Long userId);

    ReservationLifecycleResult confirm(Long reservationId, Long userId);

    ReservationLifecycleResult cancel(Long reservationId, Long userId);

    ReservationLifecycleResult refund(Long reservationId, Long userId);

    ReservationLifecycleResult refundAsAdmin(Long reservationId, Long adminUserId);

    ReservationLifecycleResult getReservation(Long reservationId, Long userId);

    int expireTimedOutHolds();
}
