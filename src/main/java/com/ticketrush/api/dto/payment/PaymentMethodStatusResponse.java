package com.ticketrush.api.dto.payment;

import com.ticketrush.application.payment.model.PaymentMethodCatalogResult;
import com.ticketrush.application.payment.model.PaymentMethodStatusResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodStatusResponse {
    private String provider;
    private String defaultMethod;
    private String providerMode;
    private boolean externalLiveEnabled;
    private List<PaymentMethodItemResponse> methods;

    public static PaymentMethodStatusResponse from(PaymentMethodCatalogResult result) {
        List<PaymentMethodItemResponse> items = result.getMethods().stream()
                .map(PaymentMethodItemResponse::from)
                .toList();
        return new PaymentMethodStatusResponse(
                result.getProvider(),
                result.getDefaultMethod(),
                result.getProviderMode(),
                result.isExternalLiveEnabled(),
                items
        );
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodItemResponse {
        private String code;
        private String label;
        private boolean enabled;
        private String status;
        private String message;

        public static PaymentMethodItemResponse from(PaymentMethodStatusResult result) {
            return new PaymentMethodItemResponse(
                    result.getCode(),
                    result.getLabel(),
                    result.isEnabled(),
                    result.getStatus(),
                    result.getMessage()
            );
        }
    }
}
