package com.ticketrush.application.payment.webhook.port.inbound;

import com.ticketrush.application.payment.webhook.model.PgReadyWebhookCommand;
import com.ticketrush.application.payment.webhook.model.PgReadyWebhookResult;

public interface PgReadyWebhookUseCase {

    PgReadyWebhookResult handle(PgReadyWebhookCommand command);
}
