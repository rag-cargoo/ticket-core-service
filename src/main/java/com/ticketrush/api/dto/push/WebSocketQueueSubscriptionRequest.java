package com.ticketrush.api.dto.push;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebSocketQueueSubscriptionRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long concertId;
}
