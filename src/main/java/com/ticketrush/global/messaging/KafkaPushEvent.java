package com.ticketrush.global.messaging;

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
public class KafkaPushEvent {

    private Type type;
    private Long userId;
    private Long concertId;
    private Long seatId;
    private Long optionId;
    private String status;
    private String eventName;
    private Object data;
    private Long ownerUserId;
    private String expiresAt;
    private String timestamp;

    public enum Type {
        QUEUE_EVENT,
        RESERVATION_STATUS,
        SEAT_MAP_STATUS
    }
}
