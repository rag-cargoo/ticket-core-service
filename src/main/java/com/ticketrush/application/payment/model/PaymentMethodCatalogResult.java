package com.ticketrush.application.payment.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodCatalogResult {
    private String provider;
    private String defaultMethod;
    private String providerMode;
    private boolean externalLiveEnabled;
    private List<PaymentMethodStatusResult> methods;
}
