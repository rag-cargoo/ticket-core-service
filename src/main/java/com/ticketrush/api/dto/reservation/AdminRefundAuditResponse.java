package com.ticketrush.api.dto.reservation;

import com.ticketrush.domain.reservation.entity.AdminRefundAuditLog;
import com.ticketrush.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundAuditResponse {
    private Long id;
    private Long reservationId;
    private Long targetUserId;
    private Long actorUserId;
    private UserRole actorRole;
    private AdminRefundAuditLog.AuditResult result;
    private String detailMessage;
    private LocalDateTime occurredAt;

    public static AdminRefundAuditResponse from(AdminRefundAuditLog auditLog) {
        return new AdminRefundAuditResponse(
                auditLog.getId(),
                auditLog.getReservationId(),
                auditLog.getTargetUserId(),
                auditLog.getActorUserId(),
                auditLog.getActorRole(),
                auditLog.getResult(),
                auditLog.getDetailMessage(),
                auditLog.getOccurredAt()
        );
    }
}
