package com.ticketrush.domain.reservation.adapter.outbound;

import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.port.outbound.ReservationSeatPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationSeatPortAdapter implements ReservationSeatPort {

    private final SeatRepository seatRepository;

    @Override
    @Transactional(readOnly = true)
    public Seat getSeat(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));
    }

    @Override
    @Transactional
    public Seat getSeatWithPessimisticLock(Long seatId) {
        return seatRepository.findByIdWithPessimisticLock(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));
    }
}
