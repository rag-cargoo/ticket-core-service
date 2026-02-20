package com.ticketrush.domain.payment.webhook;

import com.ticketrush.api.dto.payment.PgReadyWebhookRequest;
import com.ticketrush.api.dto.payment.PgReadyWebhookResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PgReadyWebhookServiceTest {

    private final PgReadyWebhookService pgReadyWebhookService = new PgReadyWebhookService();

    @Test
    void handle_shouldAcceptValidWebhookPayload() {
        PgReadyWebhookResponse response = pgReadyWebhookService.handle(new PgReadyWebhookRequest(
                "evt-123",
                "PAYMENT",
                "APPROVED",
                1001L,
                9001L,
                100_000L,
                "pg-payment-9001",
                "2026-02-21T05:00:00Z",
                "sig-abc",
                null
        ));

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getProvider()).isEqualTo("pg-ready");
        assertThat(response.getReservationId()).isEqualTo(9001L);
    }

    @Test
    void handle_shouldRejectWhenEventTypeMissing() {
        assertThatThrownBy(() -> pgReadyWebhookService.handle(new PgReadyWebhookRequest(
                "evt-123",
                "",
                "APPROVED",
                1001L,
                9001L,
                100_000L,
                "pg-payment-9001",
                "2026-02-21T05:00:00Z",
                "sig-abc",
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventType is required");
    }
}
