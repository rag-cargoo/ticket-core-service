package com.ticketrush.global.scheduler;

import com.ticketrush.api.dto.waitingqueue.WaitingQueueSsePayload;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusQuery;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusResult;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusType;
import com.ticketrush.application.waitingqueue.service.WaitingQueueService;
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

    private static final String ACTIVATE_LOCK_KEY_PREFIX = "scheduler:waiting-queue:activate:";
    private static final String HEARTBEAT_LOCK_KEY = "scheduler:waiting-queue:heartbeat";

    private final WaitingQueueService waitingQueueService;
    private final com.ticketrush.global.config.WaitingQueueProperties properties;
    private final PushNotifier pushNotifier;
    private final SchedulerLockService schedulerLockService;

    // 대기열 상위 유저를 주기적으로 활성화
    @Scheduled(fixedDelayString = "${app.waiting-queue.activation-delay-millis}")
    public void activateWaitingUsers() {
        Long concertId = properties.getActivationConcertId();
        schedulerLockService.runWithLock(ACTIVATE_LOCK_KEY_PREFIX + concertId, () -> activateWaitingUsersInternal(concertId));
    }

    private void activateWaitingUsersInternal(Long concertId) {
        log.info(">>>> [Scheduler] 대기열 유저 활성화 시작 (Concert: {}, Count: {})",
                concertId, properties.getActivationBatchSize());

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
                    buildPayload(userId, concertId, WaitingQueueStatusType.ACTIVE.name(), 0L, activeTtlSeconds)
            );
        }

        publishRankUpdates(concertId, activatedUserSet);
    }

    @Scheduled(fixedDelayString = "${app.waiting-queue.sse-heartbeat-delay-millis}")
    public void sendQueueHeartbeat() {
        schedulerLockService.runWithLock(HEARTBEAT_LOCK_KEY, pushNotifier::sendQueueHeartbeat);
    }

    private void publishRankUpdates(Long concertId, Set<Long> activatedUsers) {
        Set<Long> subscribedUsers = pushNotifier.getSubscribedQueueUsers(concertId);
        for (Long userId : subscribedUsers) {
            if (activatedUsers.contains(userId)) {
                continue;
            }

            WaitingQueueStatusResult status = waitingQueueService.getStatus(new WaitingQueueStatusQuery(userId, concertId));
            Long activeTtlSeconds = status.getStatus() == WaitingQueueStatusType.ACTIVE
                    ? waitingQueueService.getActiveTtlSeconds(userId)
                    : 0L;
            WaitingQueueSsePayload payload = buildPayload(
                    userId,
                    concertId,
                    status.getStatus().name(),
                    status.getRank(),
                    activeTtlSeconds
            );

            if (status.getStatus() == WaitingQueueStatusType.ACTIVE) {
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
