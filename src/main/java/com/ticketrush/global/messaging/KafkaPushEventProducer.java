package com.ticketrush.global.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPushEventProducer {

    private final KafkaTemplate<String, KafkaPushEvent> kafkaTemplate;

    @Value("${app.kafka.topic.push}")
    private String topic;

    public void publish(KafkaPushEvent event, String key) {
        log.debug("Publishing push event to Kafka - Topic: {}, Type: {}, Key: {}", topic, event.getType(), key);
        kafkaTemplate.send(topic, key, event);
    }
}
