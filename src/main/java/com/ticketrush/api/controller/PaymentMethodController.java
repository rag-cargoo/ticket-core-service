package com.ticketrush.api.controller;

import com.ticketrush.api.dto.payment.PaymentMethodStatusResponse;
import com.ticketrush.application.payment.port.inbound.PaymentMethodCatalogUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodCatalogUseCase paymentMethodCatalogUseCase;

    @GetMapping("/methods")
    public ResponseEntity<PaymentMethodStatusResponse> getPaymentMethods() {
        return ResponseEntity.ok(PaymentMethodStatusResponse.from(paymentMethodCatalogUseCase.getCatalog()));
    }
}
