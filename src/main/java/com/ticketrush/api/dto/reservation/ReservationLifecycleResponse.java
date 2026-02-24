package com.ticketrush.api.dto.reservation;

import com.ticketrush.application.reservation.model.ReservationLifecycleResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationLifecycleResponse {
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

    public static ReservationLifecycleResponse from(ReservationLifecycleResult result) {
        return new ReservationLifecycleResponse(
                result.getId(),
                result.getUserId(),
                result.getSeatId(),
                result.getStatus(),
                result.getReservedAt(),
                result.getHoldExpiresAt(),
                result.getConfirmedAt(),
                result.getExpiredAt(),
                result.getCancelledAt(),
                result.getRefundedAt(),
                result.getPaymentMethod(),
                result.getPaymentProvider(),
                result.getPaymentStatus(),
                result.getPaymentTransactionId(),
                result.getPaymentAction(),
                result.getPaymentRedirectUrl(),
                List.copyOf(result.getResaleActivatedUserIds())
        );
    }
}
