package com.ticketrush.application.payment.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodStatusResult {
    private String code;
    private String label;
    private boolean enabled;
    private String status;
    private String message;
}
