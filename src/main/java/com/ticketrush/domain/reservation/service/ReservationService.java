package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.ReservationResponse;

import java.util.List;

public interface ReservationService {
    ReservationResponse createReservation(ReservationRequest request);

    ReservationResponse createReservationWithPessimisticLock(ReservationRequest request);

    List<ReservationResponse> getReservationsByUserId(Long userId);

    void cancelReservation(Long reservationId);
}
