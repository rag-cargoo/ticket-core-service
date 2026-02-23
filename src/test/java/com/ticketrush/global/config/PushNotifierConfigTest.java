package com.ticketrush.global.config;

import com.ticketrush.application.port.outbound.QueueRuntimePushPort;
import com.ticketrush.application.port.outbound.ReservationStatusPushPort;
import com.ticketrush.application.port.outbound.SeatMapPushPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PushNotifierConfigTest {

    private final PushNotifierConfig config = new PushNotifierConfig();

    @Test
    void pushProperties_defaultModeShouldBeWebSocket() {
        PushProperties properties = new PushProperties();

        assertThat(properties.getMode()).isEqualTo(PushProperties.Mode.WEBSOCKET);
    }

    @Test
    void queueRuntimePushNotifier_shouldSelectSseWhenModeIsSse() {
        PushProperties properties = new PushProperties();
        properties.setMode(PushProperties.Mode.SSE);

        QueueRuntimePushPort ssePushNotifier = mock(QueueRuntimePushPort.class);
        QueueRuntimePushPort kafkaWebSocketPushNotifier = mock(QueueRuntimePushPort.class);

        QueueRuntimePushPort selected = config.queueRuntimePushNotifier(properties, ssePushNotifier, kafkaWebSocketPushNotifier);

        assertThat(selected).isSameAs(ssePushNotifier);
    }

    @Test
    void queueRuntimePushNotifier_shouldSelectKafkaWebSocketWhenModeIsWebsocket() {
        PushProperties properties = new PushProperties();
        properties.setMode(PushProperties.Mode.WEBSOCKET);

        QueueRuntimePushPort ssePushNotifier = mock(QueueRuntimePushPort.class);
        QueueRuntimePushPort kafkaWebSocketPushNotifier = mock(QueueRuntimePushPort.class);

        QueueRuntimePushPort selected = config.queueRuntimePushNotifier(properties, ssePushNotifier, kafkaWebSocketPushNotifier);

        assertThat(selected).isSameAs(kafkaWebSocketPushNotifier);
    }

    @Test
    void reservationStatusPushNotifier_shouldSelectByMode() {
        PushProperties sseProperties = new PushProperties();
        sseProperties.setMode(PushProperties.Mode.SSE);
        PushProperties wsProperties = new PushProperties();
        wsProperties.setMode(PushProperties.Mode.WEBSOCKET);

        ReservationStatusPushPort ssePushNotifier = mock(ReservationStatusPushPort.class);
        ReservationStatusPushPort kafkaWebSocketPushNotifier = mock(ReservationStatusPushPort.class);

        assertThat(config.reservationStatusPushNotifier(sseProperties, ssePushNotifier, kafkaWebSocketPushNotifier))
                .isSameAs(ssePushNotifier);
        assertThat(config.reservationStatusPushNotifier(wsProperties, ssePushNotifier, kafkaWebSocketPushNotifier))
                .isSameAs(kafkaWebSocketPushNotifier);
    }

    @Test
    void seatMapPushNotifier_shouldSelectByMode() {
        PushProperties sseProperties = new PushProperties();
        sseProperties.setMode(PushProperties.Mode.SSE);
        PushProperties wsProperties = new PushProperties();
        wsProperties.setMode(PushProperties.Mode.WEBSOCKET);

        SeatMapPushPort ssePushNotifier = mock(SeatMapPushPort.class);
        SeatMapPushPort kafkaWebSocketPushNotifier = mock(SeatMapPushPort.class);

        assertThat(config.seatMapPushNotifier(sseProperties, ssePushNotifier, kafkaWebSocketPushNotifier))
                .isSameAs(ssePushNotifier);
        assertThat(config.seatMapPushNotifier(wsProperties, ssePushNotifier, kafkaWebSocketPushNotifier))
                .isSameAs(kafkaWebSocketPushNotifier);
    }
}
