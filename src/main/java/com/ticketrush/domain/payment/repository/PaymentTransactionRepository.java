package com.ticketrush.domain.payment.repository;

import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransaction> findTopByReservationIdAndTypeAndStatusOrderByIdDesc(
            Long reservationId,
            PaymentTransactionType type,
            PaymentTransactionStatus status
    );

    List<PaymentTransaction> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
