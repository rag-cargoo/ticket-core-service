package com.ticketrush.infrastructure.reservation.adapter.outbound;

import com.ticketrush.application.waitingqueue.port.inbound.WaitingQueueRuntimeUseCase;
import com.ticketrush.domain.reservation.port.outbound.ReservationWaitingQueuePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationWaitingQueuePortAdapter implements ReservationWaitingQueuePort {

    private final WaitingQueueRuntimeUseCase waitingQueueRuntimeUseCase;

    @Override
    public List<Long> activateUsers(Long concertId, long count) {
        return waitingQueueRuntimeUseCase.activateUsers(concertId, count);
    }

    @Override
    public Long getActiveTtlSeconds(Long userId) {
        return waitingQueueRuntimeUseCase.getActiveTtlSeconds(userId);
    }
}
