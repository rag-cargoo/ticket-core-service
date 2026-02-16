package com.ticketrush.domain.reservation.adapter.outbound;

import com.ticketrush.domain.reservation.port.outbound.ReservationWaitingQueuePort;
import com.ticketrush.domain.waitingqueue.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationWaitingQueuePortAdapter implements ReservationWaitingQueuePort {

    private final WaitingQueueService waitingQueueService;

    @Override
    public List<Long> activateUsers(Long concertId, long count) {
        return waitingQueueService.activateUsers(concertId, count);
    }

    @Override
    public Long getActiveTtlSeconds(Long userId) {
        return waitingQueueService.getActiveTtlSeconds(userId);
    }
}
