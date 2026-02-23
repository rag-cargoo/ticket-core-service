package com.ticketrush.global.push;

import com.ticketrush.application.port.outbound.PushEvent;
import com.ticketrush.application.port.outbound.PushEventPublisherPort;
import com.ticketrush.application.port.outbound.QueueEventName;
import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.QueueRuntimePushPort;
import com.ticketrush.application.port.outbound.QueueSubscriberQueryPort;
import com.ticketrush.application.port.outbound.ReservationStatusPushPort;
import com.ticketrush.application.port.outbound.SeatMapPushPort;
import com.ticketrush.global.monitoring.PushMonitoringMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("kafkaWebSocketPushNotifier")
@RequiredArgsConstructor
public class KafkaWebSocketPushNotifier implements QueueRuntimePushPort, ReservationStatusPushPort, SeatMapPushPort {

    private final PushEventPublisherPort producer;
    private final QueueSubscriberQueryPort queueSubscriberQueryPort;

    @Override
    public void sendReservationStatus(Long userId, Long seatId, String status) {
        producer.publish(
                PushEvent.builder()
                        .type(PushEvent.Type.RESERVATION_STATUS)
                        .userId(userId)
                        .seatId(seatId)
                        .status(status)
                        .timestamp(Instant.now().toString())
                        .build(),
                reservationKey(userId, seatId)
        );
        PushMonitoringMetrics.increment("push", "kafka", "reservation_status");
    }

    @Override
    public void sendQueueRankUpdate(Long userId, Long concertId, QueuePushPayload data) {
        publishQueueEvent(userId, concertId, QueueEventName.RANK_UPDATE, data);
    }

    @Override
    public void sendQueueActivated(Long userId, Long concertId, QueuePushPayload data) {
        publishQueueEvent(userId, concertId, QueueEventName.ACTIVE, data);
    }

    @Override
    public void sendQueueHeartbeat() {
        String timestamp = Instant.now().toString();
        long fanoutCount = 0L;
        Map<Long, Set<Long>> snapshot = queueSubscriberQueryPort.snapshotQueueSubscribers();
        for (Map.Entry<Long, Set<Long>> entry : snapshot.entrySet()) {
            Long concertId = entry.getKey();
            for (Long userId : entry.getValue()) {
                publishQueueEvent(
                        userId,
                        concertId,
                        QueueEventName.KEEPALIVE,
                        QueuePushPayload.builder()
                                .userId(userId)
                                .concertId(concertId)
                                .timestamp(timestamp)
                                .build()
                );
                fanoutCount++;
            }
        }
        if (fanoutCount > 0L) {
            log.info("PUSH_MONITOR transport=kafka event=queue_keepalive fanout={}", fanoutCount);
        }
    }

    @Override
    public Set<Long> getSubscribedQueueUsers(Long concertId) {
        return queueSubscriberQueryPort.getSubscribedQueueUsers(concertId);
    }

    @Override
    public void sendSeatMapStatus(Long optionId, Long seatId, String status, Long ownerUserId, String expiresAt) {
        producer.publish(
                PushEvent.builder()
                        .type(PushEvent.Type.SEAT_MAP_STATUS)
                        .optionId(optionId)
                        .seatId(seatId)
                        .status(status)
                        .ownerUserId(ownerUserId)
                        .expiresAt(expiresAt)
                        .timestamp(Instant.now().toString())
                        .build(),
                seatMapKey(optionId, seatId)
        );
        PushMonitoringMetrics.increment("push", "kafka", "seat_map_status");
    }

    private void publishQueueEvent(Long userId, Long concertId, QueueEventName eventName, QueuePushPayload data) {
        producer.publish(
                PushEvent.builder()
                        .type(PushEvent.Type.QUEUE_EVENT)
                        .userId(userId)
                        .concertId(concertId)
                        .eventName(eventName)
                        .data(data)
                        .timestamp(Instant.now().toString())
                        .build(),
                queueKey(userId, concertId)
        );
        PushMonitoringMetrics.increment("push", "kafka", queueEventMetricName(eventName));
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

    private String queueEventMetricName(QueueEventName eventName) {
        return switch (eventName) {
            case RANK_UPDATE -> "queue_rank_update";
            case ACTIVE -> "queue_activated";
            case KEEPALIVE -> "queue_keepalive";
        };
    }
}
