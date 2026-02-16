package com.ticketrush.domain.reservation.port.outbound;

import java.util.List;

public interface ReservationWaitingQueuePort {
    List<Long> activateUsers(Long concertId, long count);

    Long getActiveTtlSeconds(Long userId);
}
