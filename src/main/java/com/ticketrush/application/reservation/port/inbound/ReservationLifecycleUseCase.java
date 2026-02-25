package com.ticketrush.application.reservation.port.inbound;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationLifecycleResult;

import java.util.List;

public interface ReservationLifecycleUseCase {

    ReservationLifecycleResult createHold(ReservationCreateCommand command);

    ReservationLifecycleResult startPaying(Long reservationId, Long userId);

    default ReservationLifecycleResult confirm(Long reservationId, Long userId) {
        return confirm(reservationId, userId, null);
    }

    ReservationLifecycleResult confirm(Long reservationId, Long userId, String paymentMethod);

    ReservationLifecycleResult cancel(Long reservationId, Long userId);

    List<ReservationLifecycleResult> cancelBulk(List<Long> reservationIds, Long userId);

    ReservationLifecycleResult refund(Long reservationId, Long userId);

    ReservationLifecycleResult refundAsAdmin(Long reservationId, Long adminUserId);

    ReservationLifecycleResult getReservation(Long reservationId, Long userId);

    int expireTimedOutHolds();
}
