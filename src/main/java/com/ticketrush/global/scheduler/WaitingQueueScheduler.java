package com.ticketrush.global.scheduler;

import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.QueueRuntimePushPort;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusQuery;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusResult;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusType;
import com.ticketrush.application.waitingqueue.port.outbound.WaitingQueueConfigPort;
import com.ticketrush.application.waitingqueue.port.inbound.WaitingQueueRuntimeUseCase;
import com.ticketrush.global.monitoring.PushMonitoringMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class WaitingQueueScheduler {

    private static final String ACTIVATE_LOCK_KEY_PREFIX = "scheduler:waiting-queue:activate:";
    private static final String HEARTBEAT_LOCK_KEY = "scheduler:waiting-queue:heartbeat";

    private final WaitingQueueRuntimeUseCase waitingQueueRuntimeUseCase;
    private final WaitingQueueConfigPort properties;
    private final QueueRuntimePushPort pushNotifier;
    private final SchedulerLockService schedulerLockService;

    public WaitingQueueScheduler(
            WaitingQueueRuntimeUseCase waitingQueueRuntimeUseCase,
            WaitingQueueConfigPort properties,
            @Qualifier("queueRuntimePushNotifier") QueueRuntimePushPort pushNotifier,
            SchedulerLockService schedulerLockService
    ) {
        this.waitingQueueRuntimeUseCase = waitingQueueRuntimeUseCase;
        this.properties = properties;
        this.pushNotifier = pushNotifier;
        this.schedulerLockService = schedulerLockService;
    }

    // 대기열 상위 유저를 주기적으로 활성화
    @Scheduled(fixedDelayString = "${app.waiting-queue.activation-delay-millis}")
    public void activateWaitingUsers() {
        Long concertId = properties.getActivationConcertId();
        boolean acquired = schedulerLockService.runWithLock(
                ACTIVATE_LOCK_KEY_PREFIX + concertId,
                () -> activateWaitingUsersInternal(concertId)
        );
        if (!acquired) {
            PushMonitoringMetrics.increment("queue", "scheduler", "activation_lock_miss");
            log.debug("QUEUE_MONITOR event=activation_lock_miss concertId={}", concertId);
        }
    }

    private void activateWaitingUsersInternal(Long concertId) {
        PushMonitoringMetrics.increment("queue", "scheduler", "activation_runs");
        log.info(">>>> [Scheduler] 대기열 유저 활성화 시작 (Concert: {}, Count: {})",
                concertId, properties.getActivationBatchSize());

        List<Long> activatedUsers = waitingQueueRuntimeUseCase.activateUsers(concertId, properties.getActivationBatchSize());
        if (activatedUsers.isEmpty()) {
            PushMonitoringMetrics.increment("queue", "scheduler", "activation_empty_runs");
            return;
        }
        PushMonitoringMetrics.increment("queue", "scheduler", "activated_users", activatedUsers.size());
        log.info(
                "QUEUE_MONITOR event=activation concertId={} activatedUsers={} batchSize={}",
                concertId,
                activatedUsers.size(),
                properties.getActivationBatchSize()
        );

        Set<Long> activatedUserSet = new HashSet<>(activatedUsers);

        for (Long userId : activatedUsers) {
            Long activeTtlSeconds = waitingQueueRuntimeUseCase.getActiveTtlSeconds(userId);
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
        boolean acquired = schedulerLockService.runWithLock(HEARTBEAT_LOCK_KEY, () -> {
            PushMonitoringMetrics.increment("queue", "scheduler", "heartbeat_runs");
            pushNotifier.sendQueueHeartbeat();
        });
        if (!acquired) {
            PushMonitoringMetrics.increment("queue", "scheduler", "heartbeat_lock_miss");
            log.debug("QUEUE_MONITOR event=heartbeat_lock_miss");
        }
    }

    private void publishRankUpdates(Long concertId, Set<Long> activatedUsers) {
        Set<Long> subscribedUsers = pushNotifier.getSubscribedQueueUsers(concertId);
        long activeRefreshCount = 0L;
        long rankUpdateCount = 0L;
        for (Long userId : subscribedUsers) {
            if (activatedUsers.contains(userId)) {
                continue;
            }

            WaitingQueueStatusResult status = waitingQueueRuntimeUseCase.getStatus(new WaitingQueueStatusQuery(userId, concertId));
            Long activeTtlSeconds = status.getStatus() == WaitingQueueStatusType.ACTIVE
                    ? waitingQueueRuntimeUseCase.getActiveTtlSeconds(userId)
                    : 0L;
            QueuePushPayload payload = buildPayload(
                    userId,
                    concertId,
                    status.getStatus().name(),
                    status.getRank(),
                    activeTtlSeconds
            );

            if (status.getStatus() == WaitingQueueStatusType.ACTIVE) {
                pushNotifier.sendQueueActivated(userId, concertId, payload);
                activeRefreshCount++;
            } else {
                pushNotifier.sendQueueRankUpdate(userId, concertId, payload);
                rankUpdateCount++;
            }
        }

        if (activeRefreshCount > 0L) {
            PushMonitoringMetrics.increment("queue", "scheduler", "active_refresh", activeRefreshCount);
        }
        if (rankUpdateCount > 0L) {
            PushMonitoringMetrics.increment("queue", "scheduler", "rank_updates", rankUpdateCount);
        }
        if (activeRefreshCount + rankUpdateCount > 0L) {
            log.info(
                    "QUEUE_MONITOR event=rank_refresh concertId={} activeRefresh={} rankUpdates={}",
                    concertId,
                    activeRefreshCount,
                    rankUpdateCount
            );
        }
    }

    private QueuePushPayload buildPayload(Long userId, Long concertId, String status, Long rank, Long activeTtlSeconds) {
        return QueuePushPayload.of(userId, concertId, status, rank, activeTtlSeconds);
    }
}
