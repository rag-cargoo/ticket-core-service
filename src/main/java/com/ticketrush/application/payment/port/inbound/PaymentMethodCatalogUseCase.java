package com.ticketrush.application.payment.port.inbound;

import com.ticketrush.application.payment.model.PaymentMethodCatalogResult;
import com.ticketrush.domain.payment.entity.PaymentMethod;

public interface PaymentMethodCatalogUseCase {

    PaymentMethodCatalogResult getCatalog();

    void assertMethodAvailable(PaymentMethod paymentMethod);
}
