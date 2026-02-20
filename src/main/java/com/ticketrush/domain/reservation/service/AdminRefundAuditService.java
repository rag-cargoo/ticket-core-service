package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.reservation.entity.AdminRefundAuditLog;
import com.ticketrush.domain.reservation.repository.AdminRefundAuditLogRepository;
import com.ticketrush.domain.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRefundAuditService {

    private final AdminRefundAuditLogRepository adminRefundAuditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            UserRole actorRole,
            String detailMessage
    ) {
        adminRefundAuditLogRepository.save(
                AdminRefundAuditLog.success(reservationId, targetUserId, actorUserId, actorRole, detailMessage)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDenied(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            UserRole actorRole,
            String detailMessage
    ) {
        adminRefundAuditLogRepository.save(
                AdminRefundAuditLog.denied(reservationId, targetUserId, actorUserId, actorRole, detailMessage)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailed(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            UserRole actorRole,
            String detailMessage
    ) {
        adminRefundAuditLogRepository.save(
                AdminRefundAuditLog.failed(reservationId, targetUserId, actorUserId, actorRole, detailMessage)
        );
    }

    @Transactional(readOnly = true)
    public List<AdminRefundAuditLog> getAuditLogs(
            Long reservationId,
            Long actorUserId,
            AdminRefundAuditLog.AuditResult result,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            Integer limit
    ) {
        return adminRefundAuditLogRepository.search(
                reservationId,
                actorUserId,
                result,
                fromAt,
                toAt,
                PageRequest.of(0, normalizeLimit(limit))
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }
}
