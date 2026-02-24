package com.ticketrush.application.reservation.port.outbound;

public interface PaymentConfigPort {

    String getProvider();

    long getDefaultTicketPriceAmount();

    default boolean isExternalLiveEnabled() {
        return false;
    }

    default String getPgReadyCheckoutBaseUrl() {
        return "https://pg-ready.example.com/checkout";
    }

    default String getPgReadyCallbackUrl() {
        return "/api/payments/webhooks/pg-ready";
    }

    default String getPgReadySuccessRedirectUrl() {
        return "/service?payment=success";
    }

    default String getPgReadyCancelRedirectUrl() {
        return "/service?payment=cancel";
    }

    default String getPgReadyFailureRedirectUrl() {
        return "/service?payment=failed";
    }
}
