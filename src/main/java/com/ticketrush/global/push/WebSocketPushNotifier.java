package com.ticketrush.global.push;

import com.ticketrush.global.sse.SseEventNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("webSocketPushNotifier")
@RequiredArgsConstructor
public class WebSocketPushNotifier implements PushNotifier {

    private static final String WAITING_QUEUE_TOPIC_PREFIX = "/topic/waiting-queue";
    private static final String RESERVATION_TOPIC_PREFIX = "/topic/reservations";

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, Set<Long>> queueSubscribersByConcert = new ConcurrentHashMap<>();
    private final Set<String> reservationSubscriptions = ConcurrentHashMap.newKeySet();

    public String subscribeQueue(Long userId, Long concertId) {
        queueSubscribersByConcert
                .computeIfAbsent(concertId, ignored -> ConcurrentHashMap.newKeySet())
                .add(userId);
        return queueDestination(userId, concertId);
    }

    public void unsubscribeQueue(Long userId, Long concertId) {
        Set<Long> users = queueSubscribersByConcert.get(concertId);
        if (users == null) {
            return;
        }
        users.remove(userId);
        if (users.isEmpty()) {
            queueSubscribersByConcert.remove(concertId, users);
        }
    }

    public String subscribeReservation(Long userId, Long seatId) {
        reservationSubscriptions.add(reservationKey(userId, seatId));
        return reservationDestination(userId, seatId);
    }

    public void unsubscribeReservation(Long userId, Long seatId) {
        reservationSubscriptions.remove(reservationKey(userId, seatId));
    }

    @Override
    public void sendReservationStatus(Long userId, Long seatId, String status) {
        messagingTemplate.convertAndSend(
                reservationDestination(userId, seatId),
                Map.of(
                        "event", SseEventNames.RESERVATION_STATUS,
                        "userId", userId,
                        "seatId", seatId,
                        "status", status,
                        "timestamp", Instant.now().toString()
                )
        );
    }

    @Override
    public void sendQueueRankUpdate(Long userId, Long concertId, Object data) {
        sendQueueEvent(userId, concertId, SseEventNames.RANK_UPDATE, data);
    }

    @Override
    public void sendQueueActivated(Long userId, Long concertId, Object data) {
        sendQueueEvent(userId, concertId, SseEventNames.ACTIVE, data);
    }

    @Override
    public void sendQueueHeartbeat() {
        String timestamp = Instant.now().toString();
        queueSubscribersByConcert.forEach((concertId, users) -> {
            for (Long userId : users) {
                sendQueueEvent(userId, concertId, SseEventNames.KEEPALIVE, Map.of("timestamp", timestamp));
            }
        });
    }

    @Override
    public Set<Long> getSubscribedQueueUsers(Long concertId) {
        Set<Long> users = queueSubscribersByConcert.get(concertId);
        if (users == null) {
            return Set.of();
        }
        return new HashSet<>(users);
    }

    private void sendQueueEvent(Long userId, Long concertId, String eventName, Object data) {
        messagingTemplate.convertAndSend(
                queueDestination(userId, concertId),
                Map.of(
                        "event", eventName,
                        "userId", userId,
                        "concertId", concertId,
                        "data", data
                )
        );
    }

    private String queueDestination(Long userId, Long concertId) {
        return WAITING_QUEUE_TOPIC_PREFIX + "/" + concertId + "/" + userId;
    }

    private String reservationDestination(Long userId, Long seatId) {
        return RESERVATION_TOPIC_PREFIX + "/" + seatId + "/" + userId;
    }

    private String reservationKey(Long userId, Long seatId) {
        return userId + ":" + seatId;
    }
}
