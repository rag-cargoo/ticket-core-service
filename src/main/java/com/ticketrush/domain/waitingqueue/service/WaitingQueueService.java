package com.ticketrush.domain.waitingqueue.service;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;

import java.util.List;

public interface WaitingQueueService {
    WaitingQueueResponse join(Long userId, Long concertId);
    WaitingQueueResponse getStatus(Long userId, Long concertId);
    List<Long> activateUsers(Long concertId, long count);
    Long getActiveTtlSeconds(Long userId);
}
