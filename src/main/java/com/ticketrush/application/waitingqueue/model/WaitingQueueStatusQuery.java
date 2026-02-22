package com.ticketrush.application.waitingqueue.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class WaitingQueueStatusQuery {
    private final Long userId;
    private final Long concertId;
}
