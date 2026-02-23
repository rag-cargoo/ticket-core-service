package com.ticketrush.application.reservation.model;

import java.time.LocalDateTime;

public record AdminRefundAuditRecord(
        Long id,
        Long reservationId,
        Long targetUserId,
        Long actorUserId,
        String actorRole,
        AdminRefundAuditResultType result,
        String detailMessage,
        LocalDateTime occurredAt
) {
}
