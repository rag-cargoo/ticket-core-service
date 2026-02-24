package com.ticketrush.application.reservation.model;

import com.ticketrush.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationLifecycleResult {
    private Long id;
    private Long userId;
    private Long seatId;
    private String status;
    private LocalDateTime reservedAt;
    private LocalDateTime holdExpiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime refundedAt;
    private String paymentMethod;
    private String paymentProvider;
    private String paymentStatus;
    private Long paymentTransactionId;
    private String paymentAction;
    private String paymentRedirectUrl;
    private List<Long> resaleActivatedUserIds;

    public static ReservationLifecycleResult from(Reservation reservation) {
        return from(reservation, Collections.emptyList());
    }

    public static ReservationLifecycleResult from(Reservation reservation, List<Long> resaleActivatedUserIds) {
        return new ReservationLifecycleResult(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                reservation.getStatus().name(),
                reservation.getReservedAt(),
                reservation.getHoldExpiresAt(),
                reservation.getConfirmedAt(),
                reservation.getExpiredAt(),
                reservation.getCancelledAt(),
                reservation.getRefundedAt(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.copyOf(resaleActivatedUserIds)
        );
    }

    public static ReservationLifecycleResult from(
            Reservation reservation,
            String paymentMethod,
            String paymentProvider,
            String paymentStatus,
            Long paymentTransactionId,
            String paymentAction,
            String paymentRedirectUrl
    ) {
        return new ReservationLifecycleResult(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                reservation.getStatus().name(),
                reservation.getReservedAt(),
                reservation.getHoldExpiresAt(),
                reservation.getConfirmedAt(),
                reservation.getExpiredAt(),
                reservation.getCancelledAt(),
                reservation.getRefundedAt(),
                paymentMethod,
                paymentProvider,
                paymentStatus,
                paymentTransactionId,
                paymentAction,
                paymentRedirectUrl,
                Collections.emptyList()
        );
    }
}
