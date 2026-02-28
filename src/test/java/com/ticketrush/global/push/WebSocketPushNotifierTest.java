package com.ticketrush.global.push;

import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.WebSocketQueueSubscriptionStorePort;
import com.ticketrush.application.concert.config.ConcertLiveProperties;
import com.ticketrush.global.config.WaitingQueueProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketPushNotifierTest {

    @Test
    void subscribeQueue_shouldPersistSubscriptionToStore() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketQueueSubscriptionStorePort subscriptionStore = mock(WebSocketQueueSubscriptionStorePort.class);

        WebSocketPushNotifier notifier = notifier(messagingTemplate, subscriptionStore);

        String destination = notifier.subscribeQueue(100L, 1L);

        assertThat(destination).isEqualTo("/topic/waiting-queue/1/100");
        verify(subscriptionStore).addQueueSubscriber(eq(1L), eq(100L), anyLong(), eq(300L));
    }

    @Test
    void getSubscribedQueueUsers_shouldReadFromStore() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketQueueSubscriptionStorePort subscriptionStore = mock(WebSocketQueueSubscriptionStorePort.class);
        when(subscriptionStore.getActiveSubscribers(eq(1L), anyLong()))
                .thenReturn(Set.of("100", "101"));

        WebSocketPushNotifier notifier = notifier(messagingTemplate, subscriptionStore);

        Set<Long> users = notifier.getSubscribedQueueUsers(1L);

        assertThat(users).containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void sendQueueHeartbeat_shouldPublishToSubscribedUsers() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketQueueSubscriptionStorePort subscriptionStore = mock(WebSocketQueueSubscriptionStorePort.class);
        when(subscriptionStore.getConcertIds()).thenReturn(Set.of("1"));
        when(subscriptionStore.getActiveSubscribers(eq(1L), anyLong()))
                .thenReturn(Set.of("100"));

        WebSocketPushNotifier notifier = notifier(messagingTemplate, subscriptionStore);

        notifier.sendQueueHeartbeat();

        verify(messagingTemplate).convertAndSend(eq("/topic/waiting-queue/1/100"), anyMap());
    }

    @Test
    void sendQueueActivated_shouldPublishToWaitingQueueTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketQueueSubscriptionStorePort subscriptionStore = mock(WebSocketQueueSubscriptionStorePort.class);

        WebSocketPushNotifier notifier = notifier(messagingTemplate, subscriptionStore);

        notifier.sendQueueActivated(
                100L,
                1L,
                QueuePushPayload.of(100L, 1L, "ACTIVE", 0L, 300L)
        );

        verify(messagingTemplate).convertAndSend(eq("/topic/waiting-queue/1/100"), anyMap());
    }

    @Test
    void sendReservationStatus_shouldPublishToReservationTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketQueueSubscriptionStorePort subscriptionStore = mock(WebSocketQueueSubscriptionStorePort.class);

        WebSocketPushNotifier notifier = notifier(messagingTemplate, subscriptionStore);

        notifier.sendReservationStatus(100L, 500L, "SUCCESS");

        verify(messagingTemplate).convertAndSend(eq("/topic/reservations/500/100"), anyMap());
    }

    @Test
    void sendConcertsRefresh_shouldPublishToConcertsLiveTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketQueueSubscriptionStorePort subscriptionStore = mock(WebSocketQueueSubscriptionStorePort.class);
        ConcertLivePayloadComposer payloadComposer = mock(ConcertLivePayloadComposer.class);
        when(payloadComposer.compose(any())).thenReturn(List.of());

        WebSocketPushNotifier notifier = notifier(messagingTemplate, subscriptionStore, payloadComposer, liveProperties());
        notifier.sendConcertsRefresh(77L, "2026-02-28T05:15:00Z");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass((Class) Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/concerts/live"), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("event")).isEqualTo("CONCERTS_REFRESH");
        assertThat(payload.get("optionId")).isEqualTo(77L);
        assertThat(payload.get("realtimeMode")).isEqualTo("websocket");
        assertThat(payload.get("hybridPollIntervalMillis")).isEqualTo(30000L);
        assertThat(payload.get("items")).isEqualTo(List.of());
    }

    private WebSocketPushNotifier notifier(
            SimpMessagingTemplate messagingTemplate,
            WebSocketQueueSubscriptionStorePort subscriptionStore
    ) {
        ConcertLivePayloadComposer payloadComposer = mock(ConcertLivePayloadComposer.class);
        when(payloadComposer.compose(any())).thenReturn(List.of());
        return notifier(messagingTemplate, subscriptionStore, payloadComposer, liveProperties());
    }

    private WebSocketPushNotifier notifier(
            SimpMessagingTemplate messagingTemplate,
            WebSocketQueueSubscriptionStorePort subscriptionStore,
            ConcertLivePayloadComposer payloadComposer,
            ConcertLiveProperties liveProperties
    ) {
        return new WebSocketPushNotifier(
                messagingTemplate,
                subscriptionStore,
                waitingQueueProperties(),
                payloadComposer,
                liveProperties
        );
    }

    private ConcertLiveProperties liveProperties() {
        ConcertLiveProperties properties = new ConcertLiveProperties();
        properties.setMode(ConcertLiveProperties.Mode.WEBSOCKET);
        properties.setHybridPollIntervalMillis(30000L);
        return properties;
    }

    private WaitingQueueProperties waitingQueueProperties() {
        WaitingQueueProperties properties = new WaitingQueueProperties();
        properties.setWsSubscriberZsetKeyPrefix("ws:queue:subs:");
        properties.setWsConcertIndexKey("ws:queue:concerts");
        properties.setWsSubscriberTtlSeconds(300);
        return properties;
    }
}
