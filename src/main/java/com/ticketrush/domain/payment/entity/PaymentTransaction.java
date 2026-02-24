package com.ticketrush.domain.payment.entity;

import com.ticketrush.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_transactions_idempotency_key", columnNames = {"idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_payment_transactions_user_created_at", columnList = "user_id,created_at"),
                @Index(name = "idx_payment_transactions_reservation_type", columnList = "reservation_id,type"),
                @Index(name = "idx_payment_transactions_provider_event", columnList = "payment_provider,provider_transaction_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_provider", length = 40)
    private String paymentProvider;

    @Column(name = "provider_transaction_id", length = 120)
    private String providerTransactionId;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "balance_after_amount", nullable = false)
    private Long balanceAfterAmount;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PaymentTransaction charge(
            User user,
            Long amount,
            Long balanceAfterAmount,
            String idempotencyKey,
            String description
    ) {
        return new PaymentTransaction(
                user,
                null,
                PaymentTransactionType.CHARGE,
                PaymentTransactionStatus.SUCCESS,
                PaymentMethod.WALLET,
                "wallet",
                amount,
                balanceAfterAmount,
                idempotencyKey,
                description
        );
    }

    public static PaymentTransaction payment(
            User user,
            Long reservationId,
            Long amount,
            Long balanceAfterAmount,
            String idempotencyKey,
            String description
    ) {
        return payment(
                user,
                reservationId,
                amount,
                balanceAfterAmount,
                idempotencyKey,
                description,
                PaymentTransactionStatus.SUCCESS,
                PaymentMethod.WALLET,
                "wallet"
        );
    }

    public static PaymentTransaction payment(
            User user,
            Long reservationId,
            Long amount,
            Long balanceAfterAmount,
            String idempotencyKey,
            String description,
            PaymentTransactionStatus status
    ) {
        return payment(
                user,
                reservationId,
                amount,
                balanceAfterAmount,
                idempotencyKey,
                description,
                status,
                PaymentMethod.WALLET,
                "wallet"
        );
    }

    public static PaymentTransaction payment(
            User user,
            Long reservationId,
            Long amount,
            Long balanceAfterAmount,
            String idempotencyKey,
            String description,
            PaymentTransactionStatus status,
            PaymentMethod paymentMethod,
            String paymentProvider
    ) {
        return new PaymentTransaction(
                user,
                reservationId,
                PaymentTransactionType.PAYMENT,
                status,
                paymentMethod,
                paymentProvider,
                amount,
                balanceAfterAmount,
                idempotencyKey,
                description
        );
    }

    public static PaymentTransaction refund(
            User user,
            Long reservationId,
            Long amount,
            Long balanceAfterAmount,
            String idempotencyKey,
            String description
    ) {
        return refund(
                user,
                reservationId,
                amount,
                balanceAfterAmount,
                idempotencyKey,
                description,
                PaymentMethod.WALLET,
                "wallet"
        );
    }

    public static PaymentTransaction refund(
            User user,
            Long reservationId,
            Long amount,
            Long balanceAfterAmount,
            String idempotencyKey,
            String description,
            PaymentMethod paymentMethod,
            String paymentProvider
    ) {
        return new PaymentTransaction(
                user,
                reservationId,
                PaymentTransactionType.REFUND,
                PaymentTransactionStatus.SUCCESS,
                paymentMethod,
                paymentProvider,
                amount,
                balanceAfterAmount,
                idempotencyKey,
                description
        );
    }

    private PaymentTransaction(
            User user,
            Long reservationId,
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            PaymentMethod paymentMethod,
            String paymentProvider,
            Long amount,
            Long balanceAfterAmount,
            String idempotencyKey,
            String description
    ) {
        this.user = user;
        this.reservationId = reservationId;
        this.type = type;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.paymentProvider = paymentProvider;
        this.amount = amount;
        this.balanceAfterAmount = balanceAfterAmount;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
    }

    public boolean isStatus(PaymentTransactionStatus expectedStatus) {
        return this.status == expectedStatus;
    }

    public void markSuccess(String description) {
        this.status = PaymentTransactionStatus.SUCCESS;
        this.description = description;
    }

    public void markSuccess(String description, String providerTransactionId) {
        markSuccess(description);
        setProviderTransactionId(providerTransactionId);
    }

    public void markFailed(String description) {
        this.status = PaymentTransactionStatus.FAILED;
        this.description = description;
    }

    public void markFailed(String description, String providerTransactionId) {
        markFailed(description);
        setProviderTransactionId(providerTransactionId);
    }

    private void setProviderTransactionId(String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        this.providerTransactionId = trimmed;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
