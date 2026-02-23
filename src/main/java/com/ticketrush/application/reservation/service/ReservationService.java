package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationResult;

import java.util.List;

public interface ReservationService {
    ReservationResult createReservation(ReservationCreateCommand command);

    ReservationResult createReservationWithPessimisticLock(ReservationCreateCommand command);

    List<ReservationResult> getReservationsByUserId(Long userId);

    void cancelReservation(Long reservationId);
}
