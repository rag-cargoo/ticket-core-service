package com.ticketrush.application.reservation.port.outbound;

public interface AbuseGuardConfigPort {

    long getHoldRequestWindowSeconds();

    long getHoldRequestMaxCount();

    long getDuplicateRequestWindowSeconds();

    long getDeviceWindowSeconds();

    long getDeviceMaxDistinctUsers();

    int getAuditQueryDefaultLimit();
}
