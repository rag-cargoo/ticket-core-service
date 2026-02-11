package com.ticketrush.api.dto.reservation;

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
    private List<Long> resaleActivatedUserIds;

    public static ReservationLifecycleResponse from(Reservation reservation) {
        return from(reservation, Collections.emptyList());
    }

    public static ReservationLifecycleResponse from(Reservation reservation, List<Long> resaleActivatedUserIds) {
        return new ReservationLifecycleResponse(
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
                List.copyOf(resaleActivatedUserIds)
        );
    }
}
