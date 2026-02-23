package com.ticketrush.application.payment.model;

import com.ticketrush.domain.payment.entity.PaymentTransaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResult {
    private Long id;
    private Long userId;
    private Long reservationId;
    private String type;
    private String status;
    private Long amount;
    private Long balanceAfterAmount;
    private String idempotencyKey;
    private String description;
    private LocalDateTime createdAt;

    public static PaymentTransactionResult from(PaymentTransaction transaction) {
        return new PaymentTransactionResult(
                transaction.getId(),
                transaction.getUser().getId(),
                transaction.getReservationId(),
                transaction.getType() == null ? null : transaction.getType().name(),
                transaction.getStatus() == null ? null : transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getBalanceAfterAmount(),
                transaction.getIdempotencyKey(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
