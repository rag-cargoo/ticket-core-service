package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.reservation.entity.AbuseAuditLog;
import com.ticketrush.domain.reservation.repository.AbuseAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AbuseAuditWriter {

    private final AbuseAuditLogRepository abuseAuditLogRepository;

    @Transactional
    public void saveAllowed(AbuseAuditLog auditLog) {
        abuseAuditLogRepository.save(auditLog);
    }

    /**
     * 차단 로그는 호출 트랜잭션이 롤백되더라도 남겨야 하므로 별도 트랜잭션으로 저장한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBlocked(AbuseAuditLog auditLog) {
        abuseAuditLogRepository.save(auditLog);
    }
}
