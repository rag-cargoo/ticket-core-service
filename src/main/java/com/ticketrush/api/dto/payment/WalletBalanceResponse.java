package com.ticketrush.api.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceResponse {
    private Long userId;
    private Long walletBalanceAmount;
}
