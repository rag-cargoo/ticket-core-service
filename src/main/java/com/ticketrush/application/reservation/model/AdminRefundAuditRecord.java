package com.ticketrush.application.reservation.model;

import com.ticketrush.domain.user.UserRole;

import java.time.LocalDateTime;

public record AdminRefundAuditRecord(
        Long id,
        Long reservationId,
        Long targetUserId,
        Long actorUserId,
        UserRole actorRole,
        AdminRefundAuditResultType result,
        String detailMessage,
        LocalDateTime occurredAt
) {
}
