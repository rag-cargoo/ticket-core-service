package com.ticketrush.application.reservation.model;

import java.time.LocalDateTime;

public record AbuseAuditRecord(
        Long id,
        AbuseAuditActionType action,
        AbuseAuditResultType result,
        AbuseAuditReasonType reason,
        Long userId,
        Long concertId,
        Long seatId,
        Long reservationId,
        String requestFingerprint,
        String deviceFingerprint,
        String detailMessage,
        LocalDateTime occurredAt
) {
}
