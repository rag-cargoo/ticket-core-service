package com.ticketrush.application.port.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuePushPayload {

    private Long userId;
    private Long concertId;
    private String status;
    private Long rank;
    private Long activeTtlSeconds;
    private String timestamp;

    public static QueuePushPayload of(
            Long userId,
            Long concertId,
            String status,
            Long rank,
            Long activeTtlSeconds
    ) {
        return QueuePushPayload.builder()
                .userId(userId)
                .concertId(concertId)
                .status(status)
                .rank(rank)
                .activeTtlSeconds(activeTtlSeconds)
                .timestamp(Instant.now().toString())
                .build();
    }
}
