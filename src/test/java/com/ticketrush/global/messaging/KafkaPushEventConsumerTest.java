package com.ticketrush.global.messaging;

import com.ticketrush.global.push.WebSocketPushNotifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaPushEventConsumerTest {

    @Test
    void consume_queueEvent_shouldDispatchToWebSocketNotifier() {
        WebSocketPushNotifier webSocketPushNotifier = mock(WebSocketPushNotifier.class);
        KafkaPushEventConsumer consumer = new KafkaPushEventConsumer(webSocketPushNotifier);

        KafkaPushEvent event = KafkaPushEvent.builder()
                .type(KafkaPushEvent.Type.QUEUE_EVENT)
                .userId(10L)
                .concertId(20L)
                .eventName("RANK_UPDATE")
                .data(Map.of("rank", 1))
                .build();

        consumer.consume(event);

        verify(webSocketPushNotifier).publishQueueEvent(10L, 20L, "RANK_UPDATE", Map.of("rank", 1));
    }

    @Test
    void consume_reservationStatus_shouldDispatchToWebSocketNotifier() {
        WebSocketPushNotifier webSocketPushNotifier = mock(WebSocketPushNotifier.class);
        KafkaPushEventConsumer consumer = new KafkaPushEventConsumer(webSocketPushNotifier);

        KafkaPushEvent event = KafkaPushEvent.builder()
                .type(KafkaPushEvent.Type.RESERVATION_STATUS)
                .userId(10L)
                .seatId(99L)
                .status("CONFIRMED")
                .build();

        consumer.consume(event);

        verify(webSocketPushNotifier).sendReservationStatus(10L, 99L, "CONFIRMED");
    }

    @Test
    void consume_seatMapStatus_shouldDispatchToWebSocketNotifier() {
        WebSocketPushNotifier webSocketPushNotifier = mock(WebSocketPushNotifier.class);
        KafkaPushEventConsumer consumer = new KafkaPushEventConsumer(webSocketPushNotifier);

        KafkaPushEvent event = KafkaPushEvent.builder()
                .type(KafkaPushEvent.Type.SEAT_MAP_STATUS)
                .optionId(7L)
                .seatId(55L)
                .status("SELECTING")
                .ownerUserId(100L)
                .expiresAt("2026-02-22T14:30:00Z")
                .build();

        consumer.consume(event);

        verify(webSocketPushNotifier).sendSeatMapStatus(7L, 55L, "SELECTING", 100L, "2026-02-22T14:30:00Z");
    }
}
