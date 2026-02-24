package com.ticketrush.api.dto.payment;

import com.ticketrush.application.payment.model.PaymentTransactionResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {
    private Long id;
    private Long userId;
    private Long reservationId;
    private String type;
    private String status;
    private String paymentMethod;
    private String paymentProvider;
    private String providerTransactionId;
    private Long amount;
    private Long balanceAfterAmount;
    private String idempotencyKey;
    private String description;
    private LocalDateTime createdAt;

    public static PaymentTransactionResponse from(PaymentTransactionResult transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getReservationId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getPaymentMethod(),
                transaction.getPaymentProvider(),
                transaction.getProviderTransactionId(),
                transaction.getAmount(),
                transaction.getBalanceAfterAmount(),
                transaction.getIdempotencyKey(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
