package com.ticketrush.global.monitoring;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public final class PushMonitoringMetrics {

    private static final ConcurrentMap<MetricKey, LongAdder> COUNTERS = new ConcurrentHashMap<>();

    private PushMonitoringMetrics() {
    }

    public static void increment(String domain, String transport, String event) {
        increment(domain, transport, event, 1L);
    }

    public static void increment(String domain, String transport, String event, long amount) {
        if (amount <= 0L) {
            return;
        }
        MetricKey key = new MetricKey(normalize(domain), normalize(transport), normalize(event));
        COUNTERS.computeIfAbsent(key, ignored -> new LongAdder()).add(amount);
    }

    public static Map<String, Long> snapshotAndReset() {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        for (Map.Entry<MetricKey, LongAdder> entry : COUNTERS.entrySet()) {
            long count = entry.getValue().sumThenReset();
            if (count > 0L) {
                snapshot.put(entry.getKey().toLogKey(), count);
            }
        }
        return snapshot;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private record MetricKey(String domain, String transport, String event) {
        private String toLogKey() {
            return "domain=" + domain + ",transport=" + transport + ",event=" + event;
        }
    }
}
