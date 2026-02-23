package com.ticketrush.application.waitingqueue.service;

import com.ticketrush.application.waitingqueue.port.inbound.WaitingQueueUseCase;

import java.util.List;

public interface WaitingQueueService extends WaitingQueueUseCase {

    List<Long> activateUsers(Long concertId, long count);
}
