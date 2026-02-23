package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.AbuseAuditActionType;
import com.ticketrush.application.reservation.model.AbuseAuditReasonType;
import com.ticketrush.application.reservation.model.AbuseAuditRecord;
import com.ticketrush.application.reservation.model.AbuseAuditResultType;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.entity.AbuseAuditLog;
import com.ticketrush.domain.reservation.repository.AbuseAuditLogRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.application.reservation.port.outbound.AbuseGuardConfigPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbuseAuditServiceImpl implements AbuseAuditService {

    private static final int MAX_QUERY_LIMIT = 500;

    private final AbuseAuditLogRepository abuseAuditLogRepository;
    private final AbuseAuditWriter abuseAuditWriter;
    private final AbuseGuardConfigPort abuseGuardProperties;

    @Transactional
    public void validateHoldRequest(String requestFingerprint, String deviceFingerprint, User user, Seat seat, LocalDateTime now) {
        String normalizedRequestFingerprint = normalize(requestFingerprint);
        String normalizedDeviceFingerprint = normalize(deviceFingerprint);
        ensureRateLimit(normalizedRequestFingerprint, normalizedDeviceFingerprint, user, seat, now);
        ensureDuplicateFingerprint(normalizedRequestFingerprint, normalizedDeviceFingerprint, user, seat, now);
        ensureDeviceFingerprintPolicy(normalizedRequestFingerprint, normalizedDeviceFingerprint, user, seat, now);
    }

    @Transactional
    public void recordAllowedHold(
            String requestFingerprint,
            String deviceFingerprint,
            User user,
            Seat seat,
            Long reservationId,
            LocalDateTime now
    ) {
        abuseAuditWriter.saveAllowed(
                AbuseAuditLog.allowedHold(user, seat, normalize(requestFingerprint), normalize(deviceFingerprint), reservationId, now)
        );
    }

    @Transactional(readOnly = true)
    public List<AbuseAuditRecord> getAuditLogs(
            AbuseAuditActionType action,
            AbuseAuditResultType result,
            AbuseAuditReasonType reason,
            Long userId,
            Long concertId,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            Integer limit
    ) {
        int resolvedLimit = resolveLimit(limit);
        // PostgreSQL + JPQL optional timestamp filters can fail type inference when null is bound.
        // Normalize null range params to concrete bounds so the query remains deterministic.
        LocalDateTime resolvedFromAt = fromAt == null ? LocalDateTime.of(1970, 1, 1, 0, 0, 0) : fromAt;
        LocalDateTime resolvedToAt = toAt == null ? LocalDateTime.of(9999, 12, 31, 23, 59, 59) : toAt;
        return abuseAuditLogRepository.search(
                        toDomainAction(action),
                        toDomainResult(result),
                        toDomainReason(reason),
                        userId,
                        concertId,
                        resolvedFromAt,
                        resolvedToAt,
                        PageRequest.of(0, resolvedLimit)
                )
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private void ensureRateLimit(String requestFingerprint, String deviceFingerprint, User user, Seat seat, LocalDateTime now) {
        LocalDateTime threshold = now.minusSeconds(abuseGuardProperties.getHoldRequestWindowSeconds());
        long attemptCount = abuseAuditLogRepository.countByActionAndUserIdAndOccurredAtAfter(
                AbuseAuditLog.AuditAction.HOLD_CREATE,
                user.getId(),
                threshold
        );
        if (attemptCount >= abuseGuardProperties.getHoldRequestMaxCount()) {
            throwBlocked(
                    requestFingerprint,
                    deviceFingerprint,
                    user,
                    seat,
                    AbuseAuditLog.AuditReason.RATE_LIMIT_EXCEEDED,
                    "Rate limit exceeded. userId=" + user.getId() + ", windowSeconds=" + abuseGuardProperties.getHoldRequestWindowSeconds()
            );
        }
    }

    private void ensureDuplicateFingerprint(String requestFingerprint, String deviceFingerprint, User user, Seat seat, LocalDateTime now) {
        if (requestFingerprint == null) {
            return;
        }
        LocalDateTime threshold = now.minusSeconds(abuseGuardProperties.getDuplicateRequestWindowSeconds());
        long duplicateCount = abuseAuditLogRepository.countByActionAndUserIdAndRequestFingerprintAndOccurredAtAfter(
                AbuseAuditLog.AuditAction.HOLD_CREATE,
                user.getId(),
                requestFingerprint,
                threshold
        );
        if (duplicateCount > 0) {
            throwBlocked(
                    requestFingerprint,
                    deviceFingerprint,
                    user,
                    seat,
                    AbuseAuditLog.AuditReason.DUPLICATE_REQUEST_FINGERPRINT,
                    "Duplicate request fingerprint detected. userId=" + user.getId()
                            + ", requestFingerprint=" + requestFingerprint
            );
        }
    }

    private void ensureDeviceFingerprintPolicy(String requestFingerprint, String deviceFingerprint, User user, Seat seat, LocalDateTime now) {
        if (deviceFingerprint == null) {
            return;
        }
        LocalDateTime threshold = now.minusSeconds(abuseGuardProperties.getDeviceWindowSeconds());
        long distinctUserCount = abuseAuditLogRepository.countDistinctOtherUserIdByActionAndDeviceFingerprintAndOccurredAtAfter(
                AbuseAuditLog.AuditAction.HOLD_CREATE,
                deviceFingerprint,
                user.getId(),
                threshold
        );
        if (distinctUserCount >= abuseGuardProperties.getDeviceMaxDistinctUsers()) {
            throwBlocked(
                    requestFingerprint,
                    deviceFingerprint,
                    user,
                    seat,
                    AbuseAuditLog.AuditReason.DEVICE_FINGERPRINT_MULTI_ACCOUNT,
                    "Device fingerprint used by multiple accounts. deviceFingerprint=" + deviceFingerprint
                            + ", threshold=" + abuseGuardProperties.getDeviceMaxDistinctUsers()
            );
        }
    }

    private int resolveLimit(Integer limit) {
        int defaultLimit = abuseGuardProperties.getAuditQueryDefaultLimit();
        int resolved = limit == null ? defaultLimit : limit;
        if (resolved < 1) {
            return defaultLimit;
        }
        return Math.min(resolved, MAX_QUERY_LIMIT);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void throwBlocked(
            String requestFingerprint,
            String deviceFingerprint,
            User user,
            Seat seat,
            AbuseAuditLog.AuditReason reason,
            String message
    ) {
        abuseAuditWriter.saveBlocked(
                AbuseAuditLog.blockedHold(user, seat, requestFingerprint, deviceFingerprint, reason, message, LocalDateTime.now())
        );
        throw new IllegalStateException(message);
    }

    private AbuseAuditLog.AuditAction toDomainAction(AbuseAuditActionType action) {
        if (action == null) {
            return null;
        }
        return AbuseAuditLog.AuditAction.valueOf(action.name());
    }

    private AbuseAuditLog.AuditResult toDomainResult(AbuseAuditResultType result) {
        if (result == null) {
            return null;
        }
        return AbuseAuditLog.AuditResult.valueOf(result.name());
    }

    private AbuseAuditLog.AuditReason toDomainReason(AbuseAuditReasonType reason) {
        if (reason == null) {
            return null;
        }
        return AbuseAuditLog.AuditReason.valueOf(reason.name());
    }

    private AbuseAuditRecord toRecord(AbuseAuditLog log) {
        return new AbuseAuditRecord(
                log.getId(),
                AbuseAuditActionType.valueOf(log.getAction().name()),
                AbuseAuditResultType.valueOf(log.getResult().name()),
                AbuseAuditReasonType.valueOf(log.getReason().name()),
                log.getUserId(),
                log.getConcertId(),
                log.getSeatId(),
                log.getReservationId(),
                log.getRequestFingerprint(),
                log.getDeviceFingerprint(),
                log.getDetailMessage(),
                log.getOccurredAt()
        );
    }
}
