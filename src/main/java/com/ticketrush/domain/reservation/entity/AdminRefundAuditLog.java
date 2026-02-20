package com.ticketrush.domain.reservation.entity;

import com.ticketrush.domain.user.UserRole;
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
@Table(
        name = "admin_refund_audit_logs",
        indexes = {
                @Index(name = "idx_admin_refund_reservation_time", columnList = "reservationId, occurredAt"),
                @Index(name = "idx_admin_refund_actor_time", columnList = "actorUserId, occurredAt")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRefundAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;

    private Long targetUserId;

    @Column(nullable = false)
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditResult result;

    @Column(length = 400)
    private String detailMessage;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private AdminRefundAuditLog(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            UserRole actorRole,
            AuditResult result,
            String detailMessage,
            LocalDateTime occurredAt
    ) {
        this.reservationId = reservationId;
        this.targetUserId = targetUserId;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.result = result;
        this.detailMessage = normalizeDetail(detailMessage);
        this.occurredAt = occurredAt;
    }

    public static AdminRefundAuditLog success(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            UserRole actorRole,
            String detailMessage
    ) {
        return new AdminRefundAuditLog(
                reservationId,
                targetUserId,
                actorUserId,
                actorRole,
                AuditResult.SUCCESS,
                detailMessage,
                LocalDateTime.now()
        );
    }

    public static AdminRefundAuditLog denied(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            UserRole actorRole,
            String detailMessage
    ) {
        return new AdminRefundAuditLog(
                reservationId,
                targetUserId,
                actorUserId,
                actorRole,
                AuditResult.DENIED,
                detailMessage,
                LocalDateTime.now()
        );
    }

    public static AdminRefundAuditLog failed(
            Long reservationId,
            Long targetUserId,
            Long actorUserId,
            UserRole actorRole,
            String detailMessage
    ) {
        return new AdminRefundAuditLog(
                reservationId,
                targetUserId,
                actorUserId,
                actorRole,
                AuditResult.FAILED,
                detailMessage,
                LocalDateTime.now()
        );
    }

    private static String normalizeDetail(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() <= 400) {
            return trimmed;
        }
        return trimmed.substring(0, 400);
    }

    public enum AuditResult {
        SUCCESS,
        DENIED,
        FAILED
    }
}
