package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.entity.AbuseAuditLog;
import com.ticketrush.domain.reservation.repository.AbuseAuditLogRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.global.config.AbuseGuardProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbuseAuditService {

    private static final int MAX_QUERY_LIMIT = 500;

    private final AbuseAuditLogRepository abuseAuditLogRepository;
    private final AbuseAuditWriter abuseAuditWriter;
    private final AbuseGuardProperties abuseGuardProperties;

    @Transactional
    public void validateHoldRequest(ReservationRequest request, User user, Seat seat, LocalDateTime now) {
        ensureRateLimit(request, user, seat, now);
        ensureDuplicateFingerprint(request, user, seat, now);
        ensureDeviceFingerprintPolicy(request, user, seat, now);
    }

    @Transactional
    public void recordAllowedHold(ReservationRequest request, User user, Seat seat, Long reservationId, LocalDateTime now) {
        abuseAuditWriter.saveAllowed(AbuseAuditLog.allowedHold(user, seat, request, reservationId, now));
    }

    @Transactional(readOnly = true)
    public List<AbuseAuditLog> getAuditLogs(
            AbuseAuditLog.AuditAction action,
            AbuseAuditLog.AuditResult result,
            AbuseAuditLog.AuditReason reason,
            Long userId,
            Long concertId,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            Integer limit
    ) {
        int resolvedLimit = resolveLimit(limit);
        return abuseAuditLogRepository.search(
                action,
                result,
                reason,
                userId,
                concertId,
                fromAt,
                toAt,
                PageRequest.of(0, resolvedLimit)
        );
    }

    private void ensureRateLimit(ReservationRequest request, User user, Seat seat, LocalDateTime now) {
        LocalDateTime threshold = now.minusSeconds(abuseGuardProperties.getHoldRequestWindowSeconds());
        long attemptCount = abuseAuditLogRepository.countByActionAndUserIdAndOccurredAtAfter(
                AbuseAuditLog.AuditAction.HOLD_CREATE,
                user.getId(),
                threshold
        );
        if (attemptCount >= abuseGuardProperties.getHoldRequestMaxCount()) {
            throwBlocked(
                    request,
                    user,
                    seat,
                    AbuseAuditLog.AuditReason.RATE_LIMIT_EXCEEDED,
                    "Rate limit exceeded. userId=" + user.getId() + ", windowSeconds=" + abuseGuardProperties.getHoldRequestWindowSeconds()
            );
        }
    }

    private void ensureDuplicateFingerprint(ReservationRequest request, User user, Seat seat, LocalDateTime now) {
        String requestFingerprint = normalize(request.getRequestFingerprint());
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
                    request,
                    user,
                    seat,
                    AbuseAuditLog.AuditReason.DUPLICATE_REQUEST_FINGERPRINT,
                    "Duplicate request fingerprint detected. userId=" + user.getId()
                            + ", requestFingerprint=" + requestFingerprint
            );
        }
    }

    private void ensureDeviceFingerprintPolicy(ReservationRequest request, User user, Seat seat, LocalDateTime now) {
        String deviceFingerprint = normalize(request.getDeviceFingerprint());
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
                    request,
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
            ReservationRequest request,
            User user,
            Seat seat,
            AbuseAuditLog.AuditReason reason,
            String message
    ) {
        abuseAuditWriter.saveBlocked(
                AbuseAuditLog.blockedHold(user, seat, request, reason, message, LocalDateTime.now())
        );
        throw new IllegalStateException(message);
    }
}
