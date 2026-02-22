package com.ticketrush.global.push;

import com.ticketrush.global.messaging.KafkaPushEvent;
import com.ticketrush.global.messaging.KafkaPushEventProducer;
import com.ticketrush.global.sse.SseEventNames;
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
        KafkaPushEventProducer producer = mock(KafkaPushEventProducer.class);
        WebSocketPushNotifier webSocketPushNotifier = mock(WebSocketPushNotifier.class);
        KafkaWebSocketPushNotifier notifier = new KafkaWebSocketPushNotifier(producer, webSocketPushNotifier);

        notifier.sendQueueActivated(11L, 22L, Map.of("status", "ACTIVE"));

        var eventCaptor = forClass(KafkaPushEvent.class);
        var keyCaptor = forClass(String.class);
        verify(producer).publish(eventCaptor.capture(), keyCaptor.capture());

        assertThat(eventCaptor.getValue().getType()).isEqualTo(KafkaPushEvent.Type.QUEUE_EVENT);
        assertThat(eventCaptor.getValue().getEventName()).isEqualTo(SseEventNames.ACTIVE);
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(11L);
        assertThat(eventCaptor.getValue().getConcertId()).isEqualTo(22L);
        assertThat(keyCaptor.getValue()).isEqualTo("queue:22:11");
    }

    @Test
    void sendQueueHeartbeat_shouldPublishKeepaliveForEachSubscriber() {
        KafkaPushEventProducer producer = mock(KafkaPushEventProducer.class);
        WebSocketPushNotifier webSocketPushNotifier = mock(WebSocketPushNotifier.class);
        when(webSocketPushNotifier.snapshotQueueSubscribers()).thenReturn(Map.of(1L, Set.of(10L, 11L)));

        KafkaWebSocketPushNotifier notifier = new KafkaWebSocketPushNotifier(producer, webSocketPushNotifier);
        notifier.sendQueueHeartbeat();

        verify(producer).publish(org.mockito.ArgumentMatchers.any(KafkaPushEvent.class), org.mockito.ArgumentMatchers.eq("queue:1:10"));
        verify(producer).publish(org.mockito.ArgumentMatchers.any(KafkaPushEvent.class), org.mockito.ArgumentMatchers.eq("queue:1:11"));
    }

    @Test
    void getSubscribedQueueUsers_shouldDelegateToWebSocketNotifier() {
        KafkaPushEventProducer producer = mock(KafkaPushEventProducer.class);
        WebSocketPushNotifier webSocketPushNotifier = mock(WebSocketPushNotifier.class);
        when(webSocketPushNotifier.getSubscribedQueueUsers(9L)).thenReturn(Set.of(100L));

        KafkaWebSocketPushNotifier notifier = new KafkaWebSocketPushNotifier(producer, webSocketPushNotifier);
        Set<Long> users = notifier.getSubscribedQueueUsers(9L);

        assertThat(users).containsExactly(100L);
    }
}
