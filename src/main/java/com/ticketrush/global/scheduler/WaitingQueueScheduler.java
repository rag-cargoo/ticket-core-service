package com.ticketrush.global.scheduler;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueResponse;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueSsePayload;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueStatus;
import com.ticketrush.domain.waitingqueue.service.WaitingQueueService;
import com.ticketrush.global.push.PushNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingQueueScheduler {

    private final WaitingQueueService waitingQueueService;
    private final com.ticketrush.global.config.WaitingQueueProperties properties;
    private final PushNotifier pushNotifier;

    // 대기열 상위 유저를 주기적으로 활성화
    @Scheduled(fixedDelayString = "${app.waiting-queue.activation-delay-millis}")
    public void activateWaitingUsers() {
        log.info(">>>> [Scheduler] 대기열 유저 활성화 시작 (Concert: {}, Count: {})",
                properties.getActivationConcertId(), properties.getActivationBatchSize());

        Long concertId = properties.getActivationConcertId();
        List<Long> activatedUsers = waitingQueueService.activateUsers(concertId, properties.getActivationBatchSize());
        if (activatedUsers.isEmpty()) {
            return;
        }

        Set<Long> activatedUserSet = new HashSet<>(activatedUsers);

        for (Long userId : activatedUsers) {
            Long activeTtlSeconds = waitingQueueService.getActiveTtlSeconds(userId);
            pushNotifier.sendQueueActivated(
                    userId,
                    concertId,
                    buildPayload(userId, concertId, WaitingQueueStatus.ACTIVE.name(), 0L, activeTtlSeconds)
            );
        }

        publishRankUpdates(concertId, activatedUserSet);
    }

    @Scheduled(fixedDelayString = "${app.waiting-queue.sse-heartbeat-delay-millis}")
    public void sendQueueHeartbeat() {
        pushNotifier.sendQueueHeartbeat();
    }

    private void publishRankUpdates(Long concertId, Set<Long> activatedUsers) {
        Set<Long> subscribedUsers = pushNotifier.getSubscribedQueueUsers(concertId);
        for (Long userId : subscribedUsers) {
            if (activatedUsers.contains(userId)) {
                continue;
            }

            WaitingQueueResponse status = waitingQueueService.getStatus(userId, concertId);
            Long activeTtlSeconds = WaitingQueueStatus.ACTIVE.name().equals(status.getStatus())
                    ? waitingQueueService.getActiveTtlSeconds(userId)
                    : 0L;
            WaitingQueueSsePayload payload = buildPayload(
                    userId,
                    concertId,
                    status.getStatus(),
                    status.getRank(),
                    activeTtlSeconds
            );

            if (WaitingQueueStatus.ACTIVE.name().equals(status.getStatus())) {
                pushNotifier.sendQueueActivated(userId, concertId, payload);
            } else {
                pushNotifier.sendQueueRankUpdate(userId, concertId, payload);
            }
        }
    }

    private WaitingQueueSsePayload buildPayload(Long userId, Long concertId, String status, Long rank, Long activeTtlSeconds) {
        return WaitingQueueSsePayload.builder()
                .userId(userId)
                .concertId(concertId)
                .status(status)
                .rank(rank)
                .activeTtlSeconds(activeTtlSeconds)
                .timestamp(Instant.now().toString())
                .build();
    }
}
