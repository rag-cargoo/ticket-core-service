package com.ticketrush.global.monitoring;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PushMonitoringMetricsTest {

    @Test
    void incrementAndSnapshotAndReset_shouldAggregateAndReset() {
        PushMonitoringMetrics.snapshotAndReset();

        PushMonitoringMetrics.increment("push", "kafka", "queue_activated");
        PushMonitoringMetrics.increment("push", "kafka", "queue_activated", 2L);
        PushMonitoringMetrics.increment("queue", "scheduler", "activation_runs");

        Map<String, Long> snapshot = PushMonitoringMetrics.snapshotAndReset();
        assertThat(snapshot).containsEntry("domain=push,transport=kafka,event=queue_activated", 3L);
        assertThat(snapshot).containsEntry("domain=queue,transport=scheduler,event=activation_runs", 1L);

        assertThat(PushMonitoringMetrics.snapshotAndReset()).isEmpty();
    }
}
