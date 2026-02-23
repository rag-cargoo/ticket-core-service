package com.ticketrush.api.controller;

import com.ticketrush.api.dto.payment.PgReadyWebhookRequest;
import com.ticketrush.api.dto.payment.PgReadyWebhookResponse;
import com.ticketrush.application.payment.webhook.model.PgReadyWebhookCommand;
import com.ticketrush.application.payment.webhook.model.PgReadyWebhookResult;
import com.ticketrush.application.payment.webhook.port.inbound.PgReadyWebhookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PgReadyWebhookUseCase pgReadyWebhookUseCase;

    @PostMapping("/pg-ready")
    public ResponseEntity<PgReadyWebhookResponse> receivePgReadyWebhook(
            @RequestBody PgReadyWebhookRequest request
    ) {
        PgReadyWebhookResult result = pgReadyWebhookUseCase.handle(
                new PgReadyWebhookCommand(
                        request.getProviderEventId(),
                        request.getEventType(),
                        request.getStatus(),
                        request.getUserId(),
                        request.getReservationId(),
                        request.getAmount(),
                        request.getIdempotencyKey(),
                        request.getOccurredAt(),
                        request.getSignature(),
                        request.getMetadata()
                )
        );
        return ResponseEntity.accepted().body(
                new PgReadyWebhookResponse(
                        result.getProvider(),
                        result.getEventType(),
                        result.getStatus(),
                        result.getReservationId(),
                        result.isAccepted(),
                        result.getMessage()
                )
        );
    }
}
