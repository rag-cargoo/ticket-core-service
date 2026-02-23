package com.ticketrush.application.waitingqueue.service;

import com.ticketrush.application.waitingqueue.port.outbound.WaitingQueueStore;
import com.ticketrush.global.config.WaitingQueueProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingQueueServiceImplTest {

    @Mock
    private WaitingQueueStore waitingQueueStore;

    @Mock
    private WaitingQueueProperties properties;

    @InjectMocks
    private WaitingQueueServiceImpl waitingQueueService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getQueueKeyPrefix()).thenReturn("waiting-queue:");
        lenient().when(properties.getActiveKeyPrefix()).thenReturn("active-user:");
        lenient().when(properties.getActiveTtlMinutes()).thenReturn(5L);
    }

    @Test
    void activateUsers_executesLuaScriptAtomically() {
        when(waitingQueueStore.activateUsers(
                eq("waiting-queue:7"),
                eq("active-user:"),
                eq(300L),
                eq(2L)
        )).thenReturn(List.of("1001", "1002"));

        List<Long> activatedUsers = waitingQueueService.activateUsers(7L, 2);

        assertThat(activatedUsers).containsExactly(1001L, 1002L);
        verify(waitingQueueStore).activateUsers(
                eq("waiting-queue:7"),
                eq("active-user:"),
                eq(300L),
                eq(2L)
        );
    }

    @Test
    void activateUsers_whenCountIsZero_returnsEmptyWithoutRedisCall() {
        List<Long> activatedUsers = waitingQueueService.activateUsers(7L, 0);

        assertThat(activatedUsers).isEmpty();
        verifyNoInteractions(waitingQueueStore);
    }

    @Test
    void activateUsers_whenScriptReturnsNull_returnsEmpty() {
        when(waitingQueueStore.activateUsers(any(), any(), any(Long.class), any(Long.class)))
                .thenReturn(null);

        List<Long> activatedUsers = waitingQueueService.activateUsers(7L, 2);

        assertThat(activatedUsers).isEmpty();
    }
}
