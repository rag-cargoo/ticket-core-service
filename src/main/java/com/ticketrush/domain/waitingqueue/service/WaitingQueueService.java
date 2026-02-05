package com.ticketrush.domain.waitingqueue.service;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;

public interface WaitingQueueService {
    WaitingQueueResponse join(Long userId, Long concertId);
    WaitingQueueResponse getStatus(Long userId, Long concertId);
    void activateUsers(Long concertId, long count);
}
