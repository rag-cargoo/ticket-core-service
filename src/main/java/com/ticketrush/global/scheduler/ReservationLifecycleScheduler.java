package com.ticketrush.global.scheduler;

import com.ticketrush.domain.reservation.service.ReservationLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationLifecycleScheduler {

    private static final String EXPIRE_HOLDS_LOCK_KEY = "scheduler:reservation-lifecycle:expire-holds";

    private final ReservationLifecycleService reservationLifecycleService;
    private final SchedulerLockService schedulerLockService;

    @Scheduled(fixedDelayString = "${app.reservation.expire-check-delay-millis:5000}")
    public void expireTimedOutHolds() {
        schedulerLockService.runWithLock(EXPIRE_HOLDS_LOCK_KEY, reservationLifecycleService::expireTimedOutHolds);
    }
}
