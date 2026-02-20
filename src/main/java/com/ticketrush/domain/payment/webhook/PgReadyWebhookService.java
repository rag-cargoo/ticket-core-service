package com.ticketrush.domain.payment.webhook;

import com.ticketrush.api.dto.payment.PgReadyWebhookRequest;
import com.ticketrush.api.dto.payment.PgReadyWebhookResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class PgReadyWebhookService {

    public PgReadyWebhookResponse handle(PgReadyWebhookRequest request) {
        String eventType = normalize(request.getEventType());
        String status = normalize(request.getStatus());
        Long reservationId = request.getReservationId();

        if (!StringUtils.hasText(eventType)) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("status is required");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId is required");
        }

        // Webhook 계약만 먼저 고정하고, 실제 결제승인 상태전이는 후속 task에서 연결한다.
        log.info(
                ">>>> [PgReadyWebhook] accepted eventType={}, status={}, reservationId={}, providerEventId={}",
                eventType,
                status,
                reservationId,
                normalize(request.getProviderEventId())
        );

        return new PgReadyWebhookResponse(
                "pg-ready",
                eventType,
                status,
                reservationId,
                true,
                "accepted"
        );
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }
}
