package com.ticketrush.api.dto.waitingqueue;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingQueueResponse {
    private Long userId;
    private Long concertId;
    private String status; // WAITING, ACTIVE
    private Long rank;
}
