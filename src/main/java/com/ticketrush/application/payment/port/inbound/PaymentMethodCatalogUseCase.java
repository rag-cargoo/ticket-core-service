package com.ticketrush.application.payment.port.inbound;

import com.ticketrush.application.payment.model.PaymentMethodCatalogResult;

public interface PaymentMethodCatalogUseCase {

    PaymentMethodCatalogResult getCatalog();

    void assertMethodAvailable(String paymentMethodCode);
}
