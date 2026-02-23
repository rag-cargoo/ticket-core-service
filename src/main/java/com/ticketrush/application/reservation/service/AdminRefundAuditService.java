package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.AdminRefundAuditRecord;
import com.ticketrush.application.reservation.model.AdminRefundAuditResultType;
import com.ticketrush.application.reservation.port.inbound.AdminRefundAuditUseCase;
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
public class AdminRefundAuditService implements AdminRefundAuditUseCase {

    private final AdminRefundAuditLogRepository adminRefundAuditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void recordSuccess(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            String actorRole,
            String detailMessage
    ) {
        adminRefundAuditLogRepository.save(
                AdminRefundAuditLog.success(reservationId, targetUserId, actorUserId, toDomainRole(actorRole), detailMessage)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void recordDenied(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            String actorRole,
            String detailMessage
    ) {
        adminRefundAuditLogRepository.save(
                AdminRefundAuditLog.denied(reservationId, targetUserId, actorUserId, toDomainRole(actorRole), detailMessage)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void recordFailed(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            String actorRole,
            String detailMessage
    ) {
        adminRefundAuditLogRepository.save(
                AdminRefundAuditLog.failed(reservationId, targetUserId, actorUserId, toDomainRole(actorRole), detailMessage)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<AdminRefundAuditRecord> getAuditLogs(
            Long reservationId,
            Long actorUserId,
            AdminRefundAuditResultType result,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            Integer limit
    ) {
        return adminRefundAuditLogRepository.search(
                        reservationId,
                        actorUserId,
                        toDomainResult(result),
                        fromAt,
                        toAt,
                        PageRequest.of(0, normalizeLimit(limit))
                )
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }

    private AdminRefundAuditLog.AuditResult toDomainResult(AdminRefundAuditResultType result) {
        if (result == null) {
            return null;
        }
        return AdminRefundAuditLog.AuditResult.valueOf(result.name());
    }

    private AdminRefundAuditRecord toRecord(AdminRefundAuditLog log) {
        return new AdminRefundAuditRecord(
                log.getId(),
                log.getReservationId(),
                log.getTargetUserId(),
                log.getActorUserId(),
                log.getActorRole() == null ? null : log.getActorRole().name(),
                AdminRefundAuditResultType.valueOf(log.getResult().name()),
                log.getDetailMessage(),
                log.getOccurredAt()
        );
    }

    private UserRole toDomainRole(String actorRole) {
        if (actorRole == null || actorRole.isBlank()) {
            throw new IllegalArgumentException("actorRole is required");
        }
        try {
            return UserRole.valueOf(actorRole.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid actor role: " + actorRole, exception);
        }
    }
}
