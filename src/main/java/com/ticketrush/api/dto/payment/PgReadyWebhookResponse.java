package com.ticketrush.api.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PgReadyWebhookResponse {
    private String provider;
    private String eventType;
    private String status;
    private Long reservationId;
    private boolean accepted;
    private String message;
}
