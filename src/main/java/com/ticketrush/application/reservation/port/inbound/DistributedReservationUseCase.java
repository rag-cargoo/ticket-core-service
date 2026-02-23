package com.ticketrush.application.reservation.port.inbound;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationResult;

public interface DistributedReservationUseCase {

    ReservationResult createReservation(ReservationCreateCommand command);
}
