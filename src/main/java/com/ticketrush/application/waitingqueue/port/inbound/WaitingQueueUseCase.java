package com.ticketrush.application.waitingqueue.port.inbound;

import com.ticketrush.application.waitingqueue.model.WaitingQueueJoinCommand;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusQuery;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusResult;

public interface WaitingQueueUseCase {

    WaitingQueueStatusResult join(WaitingQueueJoinCommand command);

    WaitingQueueStatusResult getStatus(WaitingQueueStatusQuery query);

    Long getActiveTtlSeconds(Long userId);
}
