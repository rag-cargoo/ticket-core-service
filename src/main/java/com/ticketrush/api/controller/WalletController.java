package com.ticketrush.api.controller;

import com.ticketrush.api.dto.payment.PaymentTransactionResponse;
import com.ticketrush.api.dto.payment.WalletBalanceResponse;
import com.ticketrush.api.dto.payment.WalletChargeRequest;
import com.ticketrush.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(new WalletBalanceResponse(userId, paymentService.getWalletBalance(userId)));
    }

    @PostMapping("/charges")
    public ResponseEntity<PaymentTransactionResponse> chargeWallet(
            @PathVariable Long userId,
            @RequestBody WalletChargeRequest request
    ) {
        return ResponseEntity.ok(PaymentTransactionResponse.from(paymentService.chargeWallet(
                userId,
                request.getAmount(),
                request.getIdempotencyKey(),
                request.getDescription()
        )));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<PaymentTransactionResponse>> getTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(
                paymentService.getTransactions(userId, limit).stream()
                        .map(PaymentTransactionResponse::from)
                        .toList()
        );
    }
}
