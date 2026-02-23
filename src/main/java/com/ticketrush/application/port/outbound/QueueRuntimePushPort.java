package com.ticketrush.application.port.outbound;

import java.util.Set;

public interface QueueRuntimePushPort {

    void sendQueueRankUpdate(Long userId, Long concertId, QueuePushPayload payload);

    void sendQueueActivated(Long userId, Long concertId, QueuePushPayload payload);

    default void sendQueueHeartbeat() {
        // Optional capability. Implementations without queue heartbeat support can ignore.
    }

    default Set<Long> getSubscribedQueueUsers(Long concertId) {
        // Optional capability. Implementations without queue subscription support can return empty.
        return Set.of();
    }
}
