package com.ticketrush.api.dto.push;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebSocketReservationSubscriptionRequest {

    // Optional legacy field. Server authority is always authenticated principal userId.
    private Long userId;

    @NotNull
    private Long seatId;
}
