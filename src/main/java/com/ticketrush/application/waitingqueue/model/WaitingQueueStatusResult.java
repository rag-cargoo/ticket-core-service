package com.ticketrush.application.waitingqueue.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingQueueStatusResult {
    private Long userId;
    private Long concertId;
    private WaitingQueueStatusType status;
    private Long rank;
}
