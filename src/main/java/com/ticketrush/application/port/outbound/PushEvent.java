package com.ticketrush.application.port.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushEvent {

    private Type type;
    private Long userId;
    private Long concertId;
    private Long seatId;
    private Long optionId;
    private String status;
    private QueueEventName eventName;
    private QueuePushPayload data;
    private Long ownerUserId;
    private String expiresAt;
    private String timestamp;

    public enum Type {
        QUEUE_EVENT,
        RESERVATION_STATUS,
        SEAT_MAP_STATUS,
        CONCERTS_REFRESH
    }
}
