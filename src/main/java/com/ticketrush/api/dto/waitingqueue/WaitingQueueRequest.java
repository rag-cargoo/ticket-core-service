package com.ticketrush.api.dto.waitingqueue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WaitingQueueRequest {
    @JsonProperty("userId")
    private Long userId;
    @JsonProperty("concertId")
    private Long concertId;

    public WaitingQueueRequest() {}

    public WaitingQueueRequest(Long userId, Long concertId) {
        this.userId = userId;
        this.concertId = concertId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getConcertId() { return concertId; }
    public void setConcertId(Long concertId) { this.concertId = concertId; }
}