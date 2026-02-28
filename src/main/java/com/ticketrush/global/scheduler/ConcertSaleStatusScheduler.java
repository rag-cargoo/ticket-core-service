package com.ticketrush.global.scheduler;

import com.ticketrush.application.concert.model.ConcertCardRuntimeSnapshot;
import com.ticketrush.application.concert.port.inbound.ConcertCardRuntimeUseCase;
import com.ticketrush.application.port.outbound.ConcertRefreshPushPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConcertSaleStatusScheduler {

    private static final String STATUS_CHECK_LOCK_KEY = "scheduler:concert-sale-status:refresh";

    private final ConcertCardRuntimeUseCase concertCardRuntimeUseCase;
    private final ConcertRefreshPushPort concertRefreshPushPort;
    private final SchedulerLockService schedulerLockService;
    private final Map<Long, String> previousSaleStatusByConcertId = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${app.concert-live.status-check-delay-millis:1000}")
    public void publishConcertRefreshOnStatusTransition() {
        boolean acquired = schedulerLockService.runWithLock(STATUS_CHECK_LOCK_KEY, this::publishInternal);
        if (!acquired) {
            log.debug("CONCERT_LIVE_MONITOR event=sale_status_lock_miss");
        }
    }

    private void publishInternal() {
        List<Long> concertIds = concertCardRuntimeUseCase.findAllConcertIds();
        if (concertIds == null || concertIds.isEmpty()) {
            previousSaleStatusByConcertId.clear();
            return;
        }

        Instant serverNow = Instant.now();
        Map<Long, ConcertCardRuntimeSnapshot> snapshotMap = concertCardRuntimeUseCase.resolveSnapshots(concertIds, serverNow);

        boolean changed = false;
        for (Long concertId : concertIds) {
            if (concertId == null) {
                continue;
            }
            ConcertCardRuntimeSnapshot snapshot = snapshotMap.get(concertId);
            String currentStatus = snapshot == null ? "UNSCHEDULED" : snapshot.getSaleStatus();
            String previousStatus = previousSaleStatusByConcertId.put(concertId, currentStatus);
            if (!Objects.equals(previousStatus, currentStatus)) {
                changed = true;
            }
        }

        Set<Long> activeConcertIds = new HashSet<>(concertIds);
        previousSaleStatusByConcertId.keySet().removeIf(id -> !activeConcertIds.contains(id));

        if (changed) {
            concertRefreshPushPort.sendConcertsRefresh(null);
            log.info("CONCERT_LIVE_MONITOR event=sale_status_transition_refreshed concertCount={}", concertIds.size());
        }
    }
}
