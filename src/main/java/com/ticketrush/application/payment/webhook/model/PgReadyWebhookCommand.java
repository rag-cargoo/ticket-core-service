package com.ticketrush.application.payment.webhook.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class PgReadyWebhookCommand {
    private String providerEventId;
    private String eventType;
    private String status;
    private Long userId;
    private Long reservationId;
    private Long amount;
    private String idempotencyKey;
    private String occurredAt;
    private String signature;
    private Map<String, Object> metadata;
}
