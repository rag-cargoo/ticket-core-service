package com.ticketrush.domain.waitingqueue.service;

import com.ticketrush.global.config.WaitingQueueProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

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
    private StringRedisTemplate redisTemplate;

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
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of("waiting-queue:7")),
                eq("active-user:"),
                eq("300"),
                eq("2")
        )).thenReturn(List.of("1001", "1002"));

        List<Long> activatedUsers = waitingQueueService.activateUsers(7L, 2);

        assertThat(activatedUsers).containsExactly(1001L, 1002L);
        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                eq(List.of("waiting-queue:7")),
                eq("active-user:"),
                eq("300"),
                eq("2")
        );
    }

    @Test
    void activateUsers_whenCountIsZero_returnsEmptyWithoutRedisCall() {
        List<Long> activatedUsers = waitingQueueService.activateUsers(7L, 0);

        assertThat(activatedUsers).isEmpty();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void activateUsers_whenScriptReturnsNull_returnsEmpty() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), any(List.class), any(), any(), any()))
                .thenReturn(null);

        List<Long> activatedUsers = waitingQueueService.activateUsers(7L, 2);

        assertThat(activatedUsers).isEmpty();
    }
}
