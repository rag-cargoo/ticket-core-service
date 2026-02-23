package com.ticketrush.application.realtime.service;

import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.SsePushPort;
import com.ticketrush.application.port.outbound.WebSocketSubscriptionPort;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusQuery;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusResult;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusType;
import com.ticketrush.application.waitingqueue.service.WaitingQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeSubscriptionServiceImplTest {

    @Mock
    private SsePushPort ssePushPort;
    @Mock
    private WebSocketSubscriptionPort webSocketSubscriptionPort;
    @Mock
    private WaitingQueueService waitingQueueService;

    private RealtimeSubscriptionServiceImpl realtimeSubscriptionService;

    @BeforeEach
    void setUp() {
        realtimeSubscriptionService = new RealtimeSubscriptionServiceImpl(
                ssePushPort,
                webSocketSubscriptionPort,
                waitingQueueService
        );
    }

    @Test
    void subscribeWaitingQueueSse_whenStatusIsActive_sendsActivatedPayload() {
        SseEmitter emitter = new SseEmitter();
        when(ssePushPort.subscribeQueue(10L, 20L)).thenReturn(emitter);
        when(waitingQueueService.getStatus(eq(new WaitingQueueStatusQuery(10L, 20L))))
                .thenReturn(WaitingQueueStatusResult.builder()
                        .userId(10L)
                        .concertId(20L)
                        .status(WaitingQueueStatusType.ACTIVE)
                        .rank(0L)
                        .build());
        when(waitingQueueService.getActiveTtlSeconds(10L)).thenReturn(50L);

        realtimeSubscriptionService.subscribeWaitingQueueSse(10L, 20L);

        verify(ssePushPort).sendQueueActivated(eq(10L), eq(20L), argThat(payloadMatches("ACTIVE", 0L, 50L)));
        verify(ssePushPort, never()).sendQueueRankUpdate(eq(10L), eq(20L), argThat(payloadMatches("ACTIVE", 0L, 50L)));
    }

    @Test
    void subscribeWaitingQueueSse_whenStatusIsWaiting_sendsRankUpdatePayload() {
        SseEmitter emitter = new SseEmitter();
        when(ssePushPort.subscribeQueue(10L, 20L)).thenReturn(emitter);
        when(waitingQueueService.getStatus(eq(new WaitingQueueStatusQuery(10L, 20L))))
                .thenReturn(WaitingQueueStatusResult.builder()
                        .userId(10L)
                        .concertId(20L)
                        .status(WaitingQueueStatusType.WAITING)
                        .rank(3L)
                        .build());

        realtimeSubscriptionService.subscribeWaitingQueueSse(10L, 20L);

        verify(ssePushPort).sendQueueRankUpdate(eq(10L), eq(20L), argThat(payloadMatches("WAITING", 3L, 0L)));
        verify(ssePushPort, never()).sendQueueActivated(eq(10L), eq(20L), argThat(payloadMatches("WAITING", 3L, 0L)));
        verify(waitingQueueService, never()).getActiveTtlSeconds(10L);
    }

    @Test
    void subscribeQueueWebSocket_delegatesToPort() {
        when(webSocketSubscriptionPort.subscribeQueue(1L, 2L)).thenReturn("/topic/waiting-queue/2/1");

        realtimeSubscriptionService.subscribeQueueWebSocket(1L, 2L);

        verify(webSocketSubscriptionPort).subscribeQueue(1L, 2L);
    }

    private ArgumentMatcher<QueuePushPayload> payloadMatches(String status, Long rank, Long ttlSeconds) {
        return payload ->
                payload != null
                        && status.equals(payload.getStatus())
                        && rank.equals(payload.getRank())
                        && ttlSeconds.equals(payload.getActiveTtlSeconds())
                        && payload.getTimestamp() != null;
    }
}
