package com.ticketrush.global.push;

import com.ticketrush.global.messaging.KafkaPushEvent;
import com.ticketrush.global.messaging.KafkaPushEventProducer;
import com.ticketrush.global.sse.SseEventNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("kafkaWebSocketPushNotifier")
@RequiredArgsConstructor
public class KafkaWebSocketPushNotifier implements PushNotifier {

    private final KafkaPushEventProducer producer;
    private final WebSocketPushNotifier webSocketPushNotifier;

    @Override
    public void sendReservationStatus(Long userId, Long seatId, String status) {
        producer.publish(
                KafkaPushEvent.builder()
                        .type(KafkaPushEvent.Type.RESERVATION_STATUS)
                        .userId(userId)
                        .seatId(seatId)
                        .status(status)
                        .timestamp(Instant.now().toString())
                        .build(),
                reservationKey(userId, seatId)
        );
    }

    @Override
    public void sendQueueRankUpdate(Long userId, Long concertId, Object data) {
        publishQueueEvent(userId, concertId, SseEventNames.RANK_UPDATE, data);
    }

    @Override
    public void sendQueueActivated(Long userId, Long concertId, Object data) {
        publishQueueEvent(userId, concertId, SseEventNames.ACTIVE, data);
    }

    @Override
    public void sendQueueHeartbeat() {
        String timestamp = Instant.now().toString();
        Map<Long, Set<Long>> snapshot = webSocketPushNotifier.snapshotQueueSubscribers();
        for (Map.Entry<Long, Set<Long>> entry : snapshot.entrySet()) {
            Long concertId = entry.getKey();
            for (Long userId : entry.getValue()) {
                publishQueueEvent(userId, concertId, SseEventNames.KEEPALIVE, Map.of("timestamp", timestamp));
            }
        }
    }

    @Override
    public Set<Long> getSubscribedQueueUsers(Long concertId) {
        return webSocketPushNotifier.getSubscribedQueueUsers(concertId);
    }

    @Override
    public void sendSeatMapStatus(Long optionId, Long seatId, String status, Long ownerUserId, String expiresAt) {
        producer.publish(
                KafkaPushEvent.builder()
                        .type(KafkaPushEvent.Type.SEAT_MAP_STATUS)
                        .optionId(optionId)
                        .seatId(seatId)
                        .status(status)
                        .ownerUserId(ownerUserId)
                        .expiresAt(expiresAt)
                        .timestamp(Instant.now().toString())
                        .build(),
                seatMapKey(optionId, seatId)
        );
    }

    private void publishQueueEvent(Long userId, Long concertId, String eventName, Object data) {
        producer.publish(
                KafkaPushEvent.builder()
                        .type(KafkaPushEvent.Type.QUEUE_EVENT)
                        .userId(userId)
                        .concertId(concertId)
                        .eventName(eventName)
                        .data(data)
                        .timestamp(Instant.now().toString())
                        .build(),
                queueKey(userId, concertId)
        );
    }

    private String queueKey(Long userId, Long concertId) {
        return "queue:" + concertId + ":" + userId;
    }

    private String reservationKey(Long userId, Long seatId) {
        return "reservation:" + seatId + ":" + userId;
    }

    private String seatMapKey(Long optionId, Long seatId) {
        return "seat-map:" + optionId + ":" + seatId;
    }
}
