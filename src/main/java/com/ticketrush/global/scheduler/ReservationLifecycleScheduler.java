package com.ticketrush.global.scheduler;

import com.ticketrush.domain.reservation.service.ReservationLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationLifecycleScheduler {

    private final ReservationLifecycleService reservationLifecycleService;

    @Scheduled(fixedDelayString = "${app.reservation.expire-check-delay-millis:5000}")
    public void expireTimedOutHolds() {
        reservationLifecycleService.expireTimedOutHolds();
    }
}
