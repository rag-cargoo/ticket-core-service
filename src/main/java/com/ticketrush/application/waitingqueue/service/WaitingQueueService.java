package com.ticketrush.application.waitingqueue.service;

import com.ticketrush.application.waitingqueue.model.WaitingQueueJoinCommand;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusQuery;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusResult;

import java.util.List;

public interface WaitingQueueService {
    WaitingQueueStatusResult join(WaitingQueueJoinCommand command);
    WaitingQueueStatusResult getStatus(WaitingQueueStatusQuery query);
    List<Long> activateUsers(Long concertId, long count);
    Long getActiveTtlSeconds(Long userId);
}
