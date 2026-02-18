package com.ticketrush.api.dto.payment;

import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
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
    private PaymentTransactionType type;
    private PaymentTransactionStatus status;
    private Long amount;
    private Long balanceAfterAmount;
    private String idempotencyKey;
    private String description;
    private LocalDateTime createdAt;

    public static PaymentTransactionResponse from(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getUser().getId(),
                transaction.getReservationId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getBalanceAfterAmount(),
                transaction.getIdempotencyKey(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
