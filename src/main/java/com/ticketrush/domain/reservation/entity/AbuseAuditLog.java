package com.ticketrush.domain.reservation.entity;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "abuse_audit_logs", indexes = {
        @Index(name = "idx_abuse_user_time", columnList = "userId, occurredAt"),
        @Index(name = "idx_abuse_device_time", columnList = "deviceFingerprint, occurredAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbuseAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditResult result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditReason reason;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long concertId;

    @Column(nullable = false)
    private Long seatId;

    private Long reservationId;

    @Column(length = 120)
    private String requestFingerprint;

    @Column(length = 120)
    private String deviceFingerprint;

    @Column(length = 400)
    private String detailMessage;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private AbuseAuditLog(
            AuditAction action,
            AuditResult result,
            AuditReason reason,
            Long userId,
            Long concertId,
            Long seatId,
            Long reservationId,
            String requestFingerprint,
            String deviceFingerprint,
            String detailMessage,
            LocalDateTime occurredAt
    ) {
        this.action = action;
        this.result = result;
        this.reason = reason;
        this.userId = userId;
        this.concertId = concertId;
        this.seatId = seatId;
        this.reservationId = reservationId;
        this.requestFingerprint = trimToNull(requestFingerprint);
        this.deviceFingerprint = trimToNull(deviceFingerprint);
        this.detailMessage = detailMessage;
        this.occurredAt = occurredAt;
    }

    public static AbuseAuditLog blockedHold(User user, Seat seat, ReservationRequest request, AuditReason reason, String detailMessage, LocalDateTime now) {
        return new AbuseAuditLog(
                AuditAction.HOLD_CREATE,
                AuditResult.BLOCKED,
                reason,
                user.getId(),
                seat.getConcertOption().getConcert().getId(),
                seat.getId(),
                null,
                request.getRequestFingerprint(),
                request.getDeviceFingerprint(),
                detailMessage,
                now
        );
    }

    public static AbuseAuditLog allowedHold(User user, Seat seat, ReservationRequest request, Long reservationId, LocalDateTime now) {
        return new AbuseAuditLog(
                AuditAction.HOLD_CREATE,
                AuditResult.ALLOWED,
                AuditReason.NONE,
                user.getId(),
                seat.getConcertOption().getConcert().getId(),
                seat.getId(),
                reservationId,
                request.getRequestFingerprint(),
                request.getDeviceFingerprint(),
                "hold accepted",
                now
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public enum AuditAction {
        HOLD_CREATE
    }

    public enum AuditResult {
        ALLOWED,
        BLOCKED
    }

    public enum AuditReason {
        NONE,
        RATE_LIMIT_EXCEEDED,
        DUPLICATE_REQUEST_FINGERPRINT,
        DEVICE_FINGERPRINT_MULTI_ACCOUNT
    }
}
