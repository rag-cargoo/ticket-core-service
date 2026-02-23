package com.ticketrush.application.reservation.port.outbound;

public interface ReservationConfigPort {

    long getHoldTtlSeconds();

    long getSoftLockTtlSeconds();

    String getSoftLockKeyPrefix();

    long getRefundCutoffHoursBeforeConcert();
}
