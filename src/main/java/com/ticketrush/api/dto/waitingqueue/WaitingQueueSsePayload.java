package com.ticketrush.api.dto.waitingqueue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingQueueSsePayload {
    private Long userId;
    private Long concertId;
    private String status;
    private Long rank;
    private Long activeTtlSeconds;
    private String timestamp;
}
