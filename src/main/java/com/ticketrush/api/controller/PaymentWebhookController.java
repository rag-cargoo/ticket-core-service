package com.ticketrush.api.controller;

import com.ticketrush.api.dto.payment.PgReadyWebhookRequest;
import com.ticketrush.api.dto.payment.PgReadyWebhookResponse;
import com.ticketrush.domain.payment.webhook.PgReadyWebhookService;
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

    private final PgReadyWebhookService pgReadyWebhookService;

    @PostMapping("/pg-ready")
    public ResponseEntity<PgReadyWebhookResponse> receivePgReadyWebhook(
            @RequestBody PgReadyWebhookRequest request
    ) {
        return ResponseEntity.accepted().body(pgReadyWebhookService.handle(request));
    }
}
