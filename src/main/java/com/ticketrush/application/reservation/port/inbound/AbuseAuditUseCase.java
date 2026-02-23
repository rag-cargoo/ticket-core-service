package com.ticketrush.application.reservation.port.inbound;

import com.ticketrush.application.reservation.model.AbuseAuditActionType;
import com.ticketrush.application.reservation.model.AbuseAuditReasonType;
import com.ticketrush.application.reservation.model.AbuseAuditRecord;
import com.ticketrush.application.reservation.model.AbuseAuditResultType;

import java.time.LocalDateTime;
import java.util.List;

public interface AbuseAuditUseCase {

    List<AbuseAuditRecord> getAuditLogs(
            AbuseAuditActionType action,
            AbuseAuditResultType result,
            AbuseAuditReasonType reason,
            Long userId,
            Long concertId,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            Integer limit
    );
}
