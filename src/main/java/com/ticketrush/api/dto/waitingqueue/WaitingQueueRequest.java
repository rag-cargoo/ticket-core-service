package com.ticketrush.api.dto.waitingqueue;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingQueueRequest {
    private Long userId;
    private Long concertId;
}
