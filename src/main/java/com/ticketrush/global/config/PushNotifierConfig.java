package com.ticketrush.global.config;

import com.ticketrush.application.port.outbound.QueueRuntimePushPort;
import com.ticketrush.application.port.outbound.ReservationStatusPushPort;
import com.ticketrush.application.port.outbound.SeatMapPushPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
@EnableConfigurationProperties(PushProperties.class)
public class PushNotifierConfig {

    @Bean
    @Primary
    public QueueRuntimePushPort queueRuntimePushNotifier(
            PushProperties properties,
            @Qualifier("ssePushNotifier") QueueRuntimePushPort ssePushNotifier,
            @Qualifier("kafkaWebSocketPushNotifier") QueueRuntimePushPort kafkaWebSocketPushNotifier
    ) {
        if (properties.getMode() == PushProperties.Mode.WEBSOCKET) {
            log.info("Queue runtime notifier mode: WEBSOCKET (Kafka fanout)");
            return kafkaWebSocketPushNotifier;
        }

        log.info("Queue runtime notifier mode: SSE");
        return ssePushNotifier;
    }

    @Bean
    @Primary
    public ReservationStatusPushPort reservationStatusPushNotifier(
            PushProperties properties,
            @Qualifier("ssePushNotifier") ReservationStatusPushPort ssePushNotifier,
            @Qualifier("kafkaWebSocketPushNotifier") ReservationStatusPushPort kafkaWebSocketPushNotifier
    ) {
        if (properties.getMode() == PushProperties.Mode.WEBSOCKET) {
            log.info("Reservation status notifier mode: WEBSOCKET (Kafka fanout)");
            return kafkaWebSocketPushNotifier;
        }

        log.info("Reservation status notifier mode: SSE");
        return ssePushNotifier;
    }

    @Bean
    @Primary
    public SeatMapPushPort seatMapPushNotifier(
            PushProperties properties,
            @Qualifier("ssePushNotifier") SeatMapPushPort ssePushNotifier,
            @Qualifier("kafkaWebSocketPushNotifier") SeatMapPushPort kafkaWebSocketPushNotifier
    ) {
        if (properties.getMode() == PushProperties.Mode.WEBSOCKET) {
            log.info("Seat-map notifier mode: WEBSOCKET (Kafka fanout)");
            return kafkaWebSocketPushNotifier;
        }

        log.info("Seat-map notifier mode: SSE");
        return ssePushNotifier;
    }
}
