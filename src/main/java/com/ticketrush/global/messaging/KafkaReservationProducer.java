package com.ticketrush.global.messaging;

import com.ticketrush.domain.reservation.event.ReservationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaReservationProducer {

    private final KafkaTemplate<String, ReservationEvent> kafkaTemplate;

    @Value("${app.kafka.topic.reservation}")
    private String topic;

    public void send(ReservationEvent event) {
        log.info("Sending reservation event to Kafka - Topic: {}, UserId: {}, SeatId: {}", 
                topic, event.getUserId(), event.getSeatId());
        
        // seatId를 메시지 키로 사용하여 동일 좌석에 대한 요청은 동일 파티션에서 순차 처리되도록 유도
        kafkaTemplate.send(topic, String.valueOf(event.getSeatId()), event);
    }
}
