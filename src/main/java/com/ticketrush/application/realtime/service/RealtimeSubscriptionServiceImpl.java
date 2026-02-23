package com.ticketrush.application.realtime.service;

import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.SsePushPort;
import com.ticketrush.application.port.outbound.WebSocketSubscriptionPort;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusQuery;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusResult;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusType;
import com.ticketrush.application.waitingqueue.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class RealtimeSubscriptionServiceImpl implements RealtimeSubscriptionService {

    private final SsePushPort ssePushPort;
    private final WebSocketSubscriptionPort webSocketSubscriptionPort;
    private final WaitingQueueService waitingQueueService;

    @Override
    public SseEmitter subscribeWaitingQueueSse(Long userId, Long concertId) {
        SseEmitter emitter = ssePushPort.subscribeQueue(userId, concertId);
        WaitingQueueStatusResult currentStatus = waitingQueueService.getStatus(new WaitingQueueStatusQuery(userId, concertId));
        Long activeTtlSeconds = currentStatus.getStatus() == WaitingQueueStatusType.ACTIVE
                ? waitingQueueService.getActiveTtlSeconds(userId)
                : 0L;

        QueuePushPayload payload = QueuePushPayload.of(
                userId,
                concertId,
                currentStatus.getStatus().name(),
                currentStatus.getRank(),
                activeTtlSeconds
        );

        if (currentStatus.getStatus() == WaitingQueueStatusType.ACTIVE) {
            ssePushPort.sendQueueActivated(userId, concertId, payload);
        } else {
            ssePushPort.sendQueueRankUpdate(userId, concertId, payload);
        }
        return emitter;
    }

    @Override
    public SseEmitter subscribeReservationSse(Long userId, Long seatId) {
        return ssePushPort.subscribeReservation(userId, seatId);
    }

    @Override
    public String subscribeQueueWebSocket(Long userId, Long concertId) {
        return webSocketSubscriptionPort.subscribeQueue(userId, concertId);
    }

    @Override
    public void unsubscribeQueueWebSocket(Long userId, Long concertId) {
        webSocketSubscriptionPort.unsubscribeQueue(userId, concertId);
    }

    @Override
    public String subscribeReservationWebSocket(Long userId, Long seatId) {
        return webSocketSubscriptionPort.subscribeReservation(userId, seatId);
    }

    @Override
    public void unsubscribeReservationWebSocket(Long userId, Long seatId) {
        webSocketSubscriptionPort.unsubscribeReservation(userId, seatId);
    }

    @Override
    public String subscribeSeatMapWebSocket(Long optionId) {
        return webSocketSubscriptionPort.subscribeSeatMap(optionId);
    }

    @Override
    public void unsubscribeSeatMapWebSocket(Long optionId) {
        webSocketSubscriptionPort.unsubscribeSeatMap(optionId);
    }
}
