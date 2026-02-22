package com.ticketrush.global.scheduler;

import com.ticketrush.domain.reservation.service.ReservationLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationLifecycleSchedulerTest {

    @Mock
    private ReservationLifecycleService reservationLifecycleService;

    @Mock
    private SchedulerLockService schedulerLockService;

    @InjectMocks
    private ReservationLifecycleScheduler reservationLifecycleScheduler;

    @Test
    void expireTimedOutHolds_delegateToService() {
        when(schedulerLockService.runWithLock(anyString(), any()))
                .thenAnswer(invocation -> {
                    Runnable task = invocation.getArgument(1);
                    task.run();
                    return true;
                });

        reservationLifecycleScheduler.expireTimedOutHolds();

        verify(schedulerLockService).runWithLock(eq("scheduler:reservation-lifecycle:expire-holds"), any());
        verify(reservationLifecycleService).expireTimedOutHolds();
    }

    @Test
    void expireTimedOutHolds_whenLockNotAcquired_skipServiceCall() {
        when(schedulerLockService.runWithLock(anyString(), any())).thenReturn(false);

        reservationLifecycleScheduler.expireTimedOutHolds();

        verify(reservationLifecycleService, never()).expireTimedOutHolds();
    }
}
