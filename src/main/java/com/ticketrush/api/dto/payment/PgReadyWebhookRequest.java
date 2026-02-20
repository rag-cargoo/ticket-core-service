package com.ticketrush.api.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PgReadyWebhookRequest {
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
