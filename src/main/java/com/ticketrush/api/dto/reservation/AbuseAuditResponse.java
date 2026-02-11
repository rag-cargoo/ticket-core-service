package com.ticketrush.api.dto.reservation;

import com.ticketrush.domain.reservation.entity.AbuseAuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AbuseAuditResponse {
    private Long id;
    private String action;
    private String result;
    private String reason;
    private Long userId;
    private Long concertId;
    private Long seatId;
    private Long reservationId;
    private String requestFingerprint;
    private String deviceFingerprint;
    private String detailMessage;
    private LocalDateTime occurredAt;

    public static AbuseAuditResponse from(AbuseAuditLog log) {
        return new AbuseAuditResponse(
                log.getId(),
                log.getAction().name(),
                log.getResult().name(),
                log.getReason().name(),
                log.getUserId(),
                log.getConcertId(),
                log.getSeatId(),
                log.getReservationId(),
                log.getRequestFingerprint(),
                log.getDeviceFingerprint(),
                log.getDetailMessage(),
                log.getOccurredAt()
        );
    }
}
