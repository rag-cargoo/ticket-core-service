package com.ticketrush.application.payment.webhook.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PgReadyWebhookResult {
    private String provider;
    private String eventType;
    private String status;
    private Long reservationId;
    private boolean accepted;
    private String message;
}
