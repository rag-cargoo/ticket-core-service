package com.ticketrush.global.push;

import com.ticketrush.application.port.outbound.PushEvent;
import com.ticketrush.application.port.outbound.PushEventPublisherPort;
import com.ticketrush.application.port.outbound.QueueEventName;
import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.QueueSubscriberQueryPort;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaWebSocketPushNotifierTest {

    @Test
    void sendQueueActivated_shouldPublishQueueEventToKafka() {
        PushEventPublisherPort producer = mock(PushEventPublisherPort.class);
        QueueSubscriberQueryPort queueSubscriberQueryPort = mock(QueueSubscriberQueryPort.class);
        KafkaWebSocketPushNotifier notifier = new KafkaWebSocketPushNotifier(producer, queueSubscriberQueryPort);

        notifier.sendQueueActivated(
                11L,
                22L,
                QueuePushPayload.of(11L, 22L, "ACTIVE", 0L, 300L)
        );

        var eventCaptor = forClass(PushEvent.class);
        var keyCaptor = forClass(String.class);
        verify(producer).publish(eventCaptor.capture(), keyCaptor.capture());

        assertThat(eventCaptor.getValue().getType()).isEqualTo(PushEvent.Type.QUEUE_EVENT);
        assertThat(eventCaptor.getValue().getEventName()).isEqualTo(QueueEventName.ACTIVE);
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(11L);
        assertThat(eventCaptor.getValue().getConcertId()).isEqualTo(22L);
        assertThat(keyCaptor.getValue()).isEqualTo("queue:22:11");
    }

    @Test
    void sendQueueHeartbeat_shouldPublishKeepaliveForEachSubscriber() {
        PushEventPublisherPort producer = mock(PushEventPublisherPort.class);
        QueueSubscriberQueryPort queueSubscriberQueryPort = mock(QueueSubscriberQueryPort.class);
        when(queueSubscriberQueryPort.snapshotQueueSubscribers()).thenReturn(Map.of(1L, Set.of(10L, 11L)));

        KafkaWebSocketPushNotifier notifier = new KafkaWebSocketPushNotifier(producer, queueSubscriberQueryPort);
        notifier.sendQueueHeartbeat();

        verify(producer).publish(org.mockito.ArgumentMatchers.any(PushEvent.class), org.mockito.ArgumentMatchers.eq("queue:1:10"));
        verify(producer).publish(org.mockito.ArgumentMatchers.any(PushEvent.class), org.mockito.ArgumentMatchers.eq("queue:1:11"));
    }

    @Test
    void getSubscribedQueueUsers_shouldDelegateToWebSocketNotifier() {
        PushEventPublisherPort producer = mock(PushEventPublisherPort.class);
        QueueSubscriberQueryPort queueSubscriberQueryPort = mock(QueueSubscriberQueryPort.class);
        when(queueSubscriberQueryPort.getSubscribedQueueUsers(9L)).thenReturn(Set.of(100L));

        KafkaWebSocketPushNotifier notifier = new KafkaWebSocketPushNotifier(producer, queueSubscriberQueryPort);
        Set<Long> users = notifier.getSubscribedQueueUsers(9L);

        assertThat(users).containsExactly(100L);
    }
}
