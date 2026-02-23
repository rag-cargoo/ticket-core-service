package com.ticketrush.application.waitingqueue.port.outbound;

public interface WaitingQueueConfigPort {

    long getMaxQueueSize();

    long getActiveTtlMinutes();

    String getQueueKeyPrefix();

    String getActiveKeyPrefix();

    default long getActivationBatchSize() {
        return 0L;
    }

    default long getActivationConcertId() {
        return 0L;
    }

    default long getSseTimeoutMillis() {
        return 300_000L;
    }

    default long getWsSubscriberTtlSeconds() {
        return 90L;
    }

    default String getWsSubscriberZsetKeyPrefix() {
        return "ws:queue:subs:";
    }

    default String getWsConcertIndexKey() {
        return "ws:queue:concerts";
    }
}
