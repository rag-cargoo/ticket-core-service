package com.ticketrush.application.demo.port.inbound;

import java.time.Instant;
import java.util.List;

public interface DemoRebalancerUseCase {

    DemoRebalancerSnapshot getSnapshot();

    DemoRebalanceTriggerResult triggerNow();

    record DemoRebalancerSnapshot(
            boolean enabled,
            int defaultIntervalMinutes,
            List<Integer> intervalOptionsMinutes,
            String serverNow,
            String cronExpression,
            String cronZone,
            String nextScheduledAt,
            DemoRebalanceJobStatus job
    ) {
    }

    record DemoRebalanceTriggerResult(
            boolean accepted,
            DemoRebalancerSnapshot snapshot
    ) {
    }

    record DemoRebalanceJobStatus(
            String jobId,
            String status,
            String phase,
            int progressPercent,
            String message,
            String startedAt,
            String finishedAt,
            String updatedAt,
            String lastLogLine
    ) {
        public static DemoRebalanceJobStatus idle() {
            String now = Instant.now().toString();
            return new DemoRebalanceJobStatus(
                    "idle",
                    "IDLE",
                    "idle",
                    0,
                    "대기 중",
                    now,
                    now,
                    now,
                    null
            );
        }

        public boolean isRunning() {
            return "RUNNING".equalsIgnoreCase(status);
        }
    }
}
