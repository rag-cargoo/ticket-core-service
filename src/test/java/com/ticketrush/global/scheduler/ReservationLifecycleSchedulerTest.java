package com.ticketrush.global.scheduler;

import com.ticketrush.domain.reservation.service.ReservationLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationLifecycleSchedulerTest {

    @Mock
    private ReservationLifecycleService reservationLifecycleService;

    @InjectMocks
    private ReservationLifecycleScheduler reservationLifecycleScheduler;

    @Test
    void expireTimedOutHolds_delegateToService() {
        reservationLifecycleScheduler.expireTimedOutHolds();
        verify(reservationLifecycleService).expireTimedOutHolds();
    }
}
