package com.ticketrush.infrastructure.messaging;

import com.ticketrush.application.port.outbound.PushEvent;
import com.ticketrush.application.port.outbound.PushEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPushEventProducer implements PushEventPublisherPort {

    private final KafkaTemplate<String, KafkaPushEvent> kafkaTemplate;

    @Value("${app.kafka.topic.push}")
    private String topic;

    @Override
    public void publish(PushEvent event, String key) {
        KafkaPushEvent kafkaEvent = toKafkaPushEvent(event);
        log.debug("Publishing push event to Kafka - Topic: {}, Type: {}, Key: {}", topic, kafkaEvent.getType(), key);
        kafkaTemplate.send(topic, key, kafkaEvent);
    }

    private KafkaPushEvent toKafkaPushEvent(PushEvent event) {
        return KafkaPushEvent.builder()
                .type(event.getType() == null ? null : KafkaPushEvent.Type.valueOf(event.getType().name()))
                .userId(event.getUserId())
                .concertId(event.getConcertId())
                .seatId(event.getSeatId())
                .optionId(event.getOptionId())
                .status(event.getStatus())
                .eventName(event.getEventName())
                .data(event.getData())
                .ownerUserId(event.getOwnerUserId())
                .expiresAt(event.getExpiresAt())
                .timestamp(event.getTimestamp())
                .build();
    }
}
