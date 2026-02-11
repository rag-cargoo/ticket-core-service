package com.ticketrush.api.dto.reservation;

import com.ticketrush.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    public static ReservationLifecycleResponse from(Reservation reservation) {
        return new ReservationLifecycleResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                reservation.getStatus().name(),
                reservation.getReservedAt(),
                reservation.getHoldExpiresAt(),
                reservation.getConfirmedAt(),
                reservation.getExpiredAt()
        );
    }
}
