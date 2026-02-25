package com.ticketrush.application.reservation.port.inbound;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationListItemResult;
import com.ticketrush.application.reservation.model.ReservationResult;

import java.util.List;

public interface ReservationUseCase {

    ReservationResult createReservation(ReservationCreateCommand command);

    ReservationResult createReservationWithPessimisticLock(ReservationCreateCommand command);

    List<ReservationResult> getReservationsByUserId(Long userId);

    List<ReservationListItemResult> getReservationsByUserId(
            Long userId,
            Long concertId,
            Long optionId,
            List<String> statuses
    );

    void cancelReservation(Long reservationId);
}
