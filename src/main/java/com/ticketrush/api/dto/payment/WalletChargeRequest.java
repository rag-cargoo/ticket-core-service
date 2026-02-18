package com.ticketrush.api.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WalletChargeRequest {
    private Long amount;
    private String idempotencyKey;
    private String description;
}
