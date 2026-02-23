package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.AbuseAuditActionType;
import com.ticketrush.application.reservation.model.AbuseAuditReasonType;
import com.ticketrush.application.reservation.model.AbuseAuditRecord;
import com.ticketrush.application.reservation.model.AbuseAuditResultType;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;

public interface AbuseAuditService {
    void validateHoldRequest(String requestFingerprint, String deviceFingerprint, User user, Seat seat, LocalDateTime now);

    void recordAllowedHold(String requestFingerprint, String deviceFingerprint, User user, Seat seat, Long reservationId, LocalDateTime now);

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
