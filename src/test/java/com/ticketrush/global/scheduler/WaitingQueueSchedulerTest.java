package com.ticketrush.global.scheduler;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;
import com.ticketrush.domain.waitingqueue.service.WaitingQueueService;
import com.ticketrush.global.config.WaitingQueueProperties;
import com.ticketrush.global.push.PushNotifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingQueueSchedulerTest {

    @Mock
    private WaitingQueueService waitingQueueService;

    @Mock
    private WaitingQueueProperties properties;

    @Mock
    private PushNotifier pushNotifier;

    @InjectMocks
    private WaitingQueueScheduler waitingQueueScheduler;

    @Test
    void activateWaitingUsers_sendActiveAndRankUpdates() {
        when(properties.getActivationConcertId()).thenReturn(1L);
        when(properties.getActivationBatchSize()).thenReturn(10L);

        when(waitingQueueService.activateUsers(1L, 10L)).thenReturn(List.of(101L));
        when(waitingQueueService.getActiveTtlSeconds(101L)).thenReturn(280L);
        when(pushNotifier.getSubscribedQueueUsers(1L)).thenReturn(Set.of(101L, 102L));
        when(waitingQueueService.getStatus(102L, 1L)).thenReturn(
                WaitingQueueResponse.builder()
                        .userId(102L)
                        .concertId(1L)
                        .status("WAITING")
                        .rank(5L)
                        .build()
        );

        waitingQueueScheduler.activateWaitingUsers();

        verify(pushNotifier).sendQueueActivated(eq(101L), eq(1L), any());
        verify(pushNotifier).sendQueueRankUpdate(eq(102L), eq(1L), any());
        verify(waitingQueueService, never()).getStatus(101L, 1L);
    }

    @Test
    void sendQueueHeartbeat_delegateToPushNotifier() {
        waitingQueueScheduler.sendQueueHeartbeat();
        verify(pushNotifier).sendQueueHeartbeat();
    }
}
