package com.ticketrush.api.dto.reservation;

import com.ticketrush.application.reservation.model.AbuseAuditRecord;
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

    public static AbuseAuditResponse from(AbuseAuditRecord log) {
        return new AbuseAuditResponse(
                log.id(),
                log.action().name(),
                log.result().name(),
                log.reason().name(),
                log.userId(),
                log.concertId(),
                log.seatId(),
                log.reservationId(),
                log.requestFingerprint(),
                log.deviceFingerprint(),
                log.detailMessage(),
                log.occurredAt()
        );
    }
}
