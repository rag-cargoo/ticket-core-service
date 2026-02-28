package com.ticketrush.infrastructure.messaging;

import com.ticketrush.application.port.outbound.QueueEventName;
import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.WebSocketEventDispatchPort;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaPushEventConsumerTest {

    @Test
    void consume_queueEvent_shouldDispatchToWebSocketNotifier() {
        WebSocketEventDispatchPort webSocketEventDispatchPort = mock(WebSocketEventDispatchPort.class);
        KafkaPushEventConsumer consumer = new KafkaPushEventConsumer(webSocketEventDispatchPort);

        KafkaPushEvent event = KafkaPushEvent.builder()
                .type(KafkaPushEvent.Type.QUEUE_EVENT)
                .userId(10L)
                .concertId(20L)
                .eventName(QueueEventName.RANK_UPDATE)
                .data(QueuePushPayload.of(10L, 20L, "WAITING", 1L, 0L))
                .build();

        consumer.consume(event);

        verify(webSocketEventDispatchPort).publishQueueEvent(
                eq(10L),
                eq(20L),
                eq(QueueEventName.RANK_UPDATE),
                argThat(payload ->
                        payload != null
                                && payload.getUserId().equals(10L)
                                && payload.getConcertId().equals(20L)
                                && payload.getStatus().equals("WAITING")
                                && payload.getRank().equals(1L)
                )
        );
    }

    @Test
    void consume_reservationStatus_shouldDispatchToWebSocketNotifier() {
        WebSocketEventDispatchPort webSocketEventDispatchPort = mock(WebSocketEventDispatchPort.class);
        KafkaPushEventConsumer consumer = new KafkaPushEventConsumer(webSocketEventDispatchPort);

        KafkaPushEvent event = KafkaPushEvent.builder()
                .type(KafkaPushEvent.Type.RESERVATION_STATUS)
                .userId(10L)
                .seatId(99L)
                .status("CONFIRMED")
                .build();

        consumer.consume(event);

        verify(webSocketEventDispatchPort).sendReservationStatus(10L, 99L, "CONFIRMED");
    }

    @Test
    void consume_seatMapStatus_shouldDispatchToWebSocketNotifier() {
        WebSocketEventDispatchPort webSocketEventDispatchPort = mock(WebSocketEventDispatchPort.class);
        KafkaPushEventConsumer consumer = new KafkaPushEventConsumer(webSocketEventDispatchPort);

        KafkaPushEvent event = KafkaPushEvent.builder()
                .type(KafkaPushEvent.Type.SEAT_MAP_STATUS)
                .optionId(7L)
                .seatId(55L)
                .status("SELECTING")
                .ownerUserId(100L)
                .expiresAt("2026-02-22T14:30:00Z")
                .build();

        consumer.consume(event);

        verify(webSocketEventDispatchPort).sendSeatMapStatus(7L, 55L, "SELECTING", 100L, "2026-02-22T14:30:00Z");
    }

    @Test
    void consume_concertsRefresh_shouldDispatchToWebSocketNotifier() {
        WebSocketEventDispatchPort webSocketEventDispatchPort = mock(WebSocketEventDispatchPort.class);
        KafkaPushEventConsumer consumer = new KafkaPushEventConsumer(webSocketEventDispatchPort);

        KafkaPushEvent event = KafkaPushEvent.builder()
                .type(KafkaPushEvent.Type.CONCERTS_REFRESH)
                .optionId(77L)
                .timestamp("2026-02-28T05:10:00Z")
                .build();

        consumer.consume(event);

        verify(webSocketEventDispatchPort).sendConcertsRefresh(77L, "2026-02-28T05:10:00Z");
    }
}
