package com.ticketrush.global.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PushMonitoringSnapshotScheduler {

    @Scheduled(fixedDelayString = "${app.push.monitor.snapshot-delay-millis:60000}")
    public void logSnapshot() {
        Map<String, Long> snapshot = PushMonitoringMetrics.snapshotAndReset();
        if (snapshot.isEmpty()) {
            return;
        }

        long total = snapshot.values().stream().mapToLong(Long::longValue).sum();
        log.info("PUSH_MONITOR_SNAPSHOT total={} metrics={}", total, snapshot);
    }
}
