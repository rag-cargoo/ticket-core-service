package com.ticketrush.application.reservation.port.inbound;

import com.ticketrush.application.reservation.model.AdminRefundAuditRecord;
import com.ticketrush.application.reservation.model.AdminRefundAuditResultType;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminRefundAuditUseCase {

    void recordSuccess(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            String actorRole,
            String detailMessage
    );

    void recordDenied(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            String actorRole,
            String detailMessage
    );

    void recordFailed(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            String actorRole,
            String detailMessage
    );

    List<AdminRefundAuditRecord> getAuditLogs(
            Long reservationId,
            Long actorUserId,
            AdminRefundAuditResultType result,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            Integer limit
    );
}
