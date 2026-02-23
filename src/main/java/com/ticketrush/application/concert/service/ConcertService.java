package com.ticketrush.application.concert.service;

import com.ticketrush.application.concert.port.inbound.ConcertUseCase;
import com.ticketrush.domain.concert.entity.Seat;

public interface ConcertService extends ConcertUseCase {

    Seat getSeat(Long seatId);

    Seat getSeatWithPessimisticLock(Long seatId);
}
