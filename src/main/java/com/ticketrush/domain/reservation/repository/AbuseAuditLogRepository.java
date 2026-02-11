package com.ticketrush.domain.reservation.repository;

import com.ticketrush.domain.reservation.entity.AbuseAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AbuseAuditLogRepository extends JpaRepository<AbuseAuditLog, Long> {

    long countByActionAndUserIdAndOccurredAtAfter(
            AbuseAuditLog.AuditAction action,
            Long userId,
            LocalDateTime occurredAt
    );

    long countByActionAndUserIdAndRequestFingerprintAndOccurredAtAfter(
            AbuseAuditLog.AuditAction action,
            Long userId,
            String requestFingerprint,
            LocalDateTime occurredAt
    );

    @Query("""
            select count(distinct l.userId)
            from AbuseAuditLog l
            where l.action = :action
              and l.deviceFingerprint = :deviceFingerprint
              and l.userId <> :excludedUserId
              and l.occurredAt >= :occurredAt
            """)
    long countDistinctOtherUserIdByActionAndDeviceFingerprintAndOccurredAtAfter(
            @Param("action") AbuseAuditLog.AuditAction action,
            @Param("deviceFingerprint") String deviceFingerprint,
            @Param("excludedUserId") Long excludedUserId,
            @Param("occurredAt") LocalDateTime occurredAt
    );

    @Query("""
            select l
            from AbuseAuditLog l
            where (:action is null or l.action = :action)
              and (:result is null or l.result = :result)
              and (:reason is null or l.reason = :reason)
              and (:userId is null or l.userId = :userId)
              and (:concertId is null or l.concertId = :concertId)
              and (:fromAt is null or l.occurredAt >= :fromAt)
              and (:toAt is null or l.occurredAt <= :toAt)
            order by l.occurredAt desc, l.id desc
            """)
    List<AbuseAuditLog> search(
            @Param("action") AbuseAuditLog.AuditAction action,
            @Param("result") AbuseAuditLog.AuditResult result,
            @Param("reason") AbuseAuditLog.AuditReason reason,
            @Param("userId") Long userId,
            @Param("concertId") Long concertId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            Pageable pageable
    );
}
