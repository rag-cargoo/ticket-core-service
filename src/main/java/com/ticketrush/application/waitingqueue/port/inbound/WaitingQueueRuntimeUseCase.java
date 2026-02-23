package com.ticketrush.application.waitingqueue.port.inbound;

import java.util.List;

public interface WaitingQueueRuntimeUseCase extends WaitingQueueUseCase {

    List<Long> activateUsers(Long concertId, long count);
}
