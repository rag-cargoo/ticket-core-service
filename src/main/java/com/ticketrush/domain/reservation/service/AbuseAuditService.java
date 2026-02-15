package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.entity.AbuseAuditLog;
import com.ticketrush.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;

public interface AbuseAuditService {
    void validateHoldRequest(ReservationRequest request, User user, Seat seat, LocalDateTime now);

    void recordAllowedHold(ReservationRequest request, User user, Seat seat, Long reservationId, LocalDateTime now);

    List<AbuseAuditLog> getAuditLogs(
            AbuseAuditLog.AuditAction action,
            AbuseAuditLog.AuditResult result,
            AbuseAuditLog.AuditReason reason,
            Long userId,
            Long concertId,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            Integer limit
    );
}
