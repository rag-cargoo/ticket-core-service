package com.ticketrush.global.messaging;

import com.ticketrush.global.push.WebSocketPushNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPushEventConsumer {

    private final WebSocketPushNotifier webSocketPushNotifier;

    @KafkaListener(
            topics = "${app.kafka.topic.push}",
            groupId = "${app.kafka.push.consumer-group-id:${spring.application.name:ticket-core-service}-${random.uuid}}",
            properties = {
                    "auto.offset.reset=latest"
            }
    )
    public void consume(KafkaPushEvent event) {
        if (event == null || event.getType() == null) {
            return;
        }

        switch (event.getType()) {
            case QUEUE_EVENT -> webSocketPushNotifier.publishQueueEvent(
                    event.getUserId(),
                    event.getConcertId(),
                    event.getEventName(),
                    event.getData()
            );
            case RESERVATION_STATUS -> webSocketPushNotifier.sendReservationStatus(
                    event.getUserId(),
                    event.getSeatId(),
                    event.getStatus()
            );
            case SEAT_MAP_STATUS -> webSocketPushNotifier.sendSeatMapStatus(
                    event.getOptionId(),
                    event.getSeatId(),
                    event.getStatus(),
                    event.getOwnerUserId(),
                    event.getExpiresAt()
            );
            default -> log.debug("Unsupported push event type: {}", event.getType());
        }
    }
}
