package com.ticketrush.api.dto.reservation;

import com.ticketrush.application.reservation.model.AdminRefundAuditRecord;
import com.ticketrush.application.reservation.model.AdminRefundAuditResultType;
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
    private AdminRefundAuditResultType result;
    private String detailMessage;
    private LocalDateTime occurredAt;

    public static AdminRefundAuditResponse from(AdminRefundAuditRecord auditLog) {
        return new AdminRefundAuditResponse(
                auditLog.id(),
                auditLog.reservationId(),
                auditLog.targetUserId(),
                auditLog.actorUserId(),
                auditLog.actorRole(),
                auditLog.result(),
                auditLog.detailMessage(),
                auditLog.occurredAt()
        );
    }
}
