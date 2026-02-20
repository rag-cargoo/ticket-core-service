package com.ticketrush.domain.reservation.repository;

import com.ticketrush.domain.reservation.entity.AdminRefundAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminRefundAuditLogRepository extends JpaRepository<AdminRefundAuditLog, Long> {

    @Query("""
            select l
            from AdminRefundAuditLog l
            where (:reservationId is null or l.reservationId = :reservationId)
              and (:actorUserId is null or l.actorUserId = :actorUserId)
              and (:result is null or l.result = :result)
              and l.occurredAt >= coalesce(:fromAt, l.occurredAt)
              and l.occurredAt <= coalesce(:toAt, l.occurredAt)
            order by l.occurredAt desc, l.id desc
            """)
    List<AdminRefundAuditLog> search(
            @Param("reservationId") Long reservationId,
            @Param("actorUserId") Long actorUserId,
            @Param("result") AdminRefundAuditLog.AuditResult result,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            Pageable pageable
    );
}
