package com.ticketrush.global.push;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketPushNotifierTest {

    @Test
    void subscribeQueue_shouldTrackSubscribedUsersByConcert() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketPushNotifier notifier = new WebSocketPushNotifier(messagingTemplate);

        String destination = notifier.subscribeQueue(100L, 1L);

        assertThat(destination).isEqualTo("/topic/waiting-queue/1/100");
        assertThat(notifier.getSubscribedQueueUsers(1L)).isEqualTo(Set.of(100L));
    }

    @Test
    void sendQueueActivated_shouldPublishToWaitingQueueTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketPushNotifier notifier = new WebSocketPushNotifier(messagingTemplate);

        notifier.sendQueueActivated(100L, 1L, Map.of("status", "ACTIVE"));

        verify(messagingTemplate).convertAndSend(eq("/topic/waiting-queue/1/100"), anyMap());
    }

    @Test
    void sendReservationStatus_shouldPublishToReservationTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketPushNotifier notifier = new WebSocketPushNotifier(messagingTemplate);

        notifier.sendReservationStatus(100L, 500L, "SUCCESS");

        verify(messagingTemplate).convertAndSend(eq("/topic/reservations/500/100"), anyMap());
    }
}
