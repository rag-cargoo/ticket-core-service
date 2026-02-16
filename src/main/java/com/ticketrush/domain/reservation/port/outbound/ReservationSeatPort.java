package com.ticketrush.domain.reservation.port.outbound;

import com.ticketrush.domain.concert.entity.Seat;

public interface ReservationSeatPort {
    Seat getSeat(Long seatId);

    Seat getSeatWithPessimisticLock(Long seatId);
}
