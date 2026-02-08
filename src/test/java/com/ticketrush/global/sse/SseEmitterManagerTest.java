package com.ticketrush.global.sse;

import com.ticketrush.global.config.WaitingQueueProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SseEmitterManagerTest {

    @Test
    void subscribeQueue_shouldReplaceEmitterOnReconnect() {
        SseEmitterManager manager = new SseEmitterManager(waitingQueueProperties());

        manager.subscribeQueue(100L, 1L);
        manager.subscribeQueue(100L, 1L); // reconnect

        assertThat(manager.getSubscribedQueueUsers(1L)).containsExactly(100L);
    }

    @Test
    void getSubscribedQueueUsers_shouldFilterByConcertId() {
        SseEmitterManager manager = new SseEmitterManager(waitingQueueProperties());

        manager.subscribeQueue(100L, 1L);
        manager.subscribeQueue(101L, 1L);
        manager.subscribeQueue(200L, 2L);

        assertThat(manager.getSubscribedQueueUsers(1L)).containsExactlyInAnyOrder(100L, 101L);
        assertThat(manager.getSubscribedQueueUsers(2L)).containsExactly(200L);
    }

    @Test
    void sendQueueHeartbeat_shouldNotThrow() {
        SseEmitterManager manager = new SseEmitterManager(waitingQueueProperties());
        manager.subscribeQueue(100L, 1L);

        assertThatCode(manager::sendQueueHeartbeat).doesNotThrowAnyException();
    }

    private WaitingQueueProperties waitingQueueProperties() {
        WaitingQueueProperties properties = new WaitingQueueProperties();
        properties.setSseTimeoutMillis(300_000L);
        return properties;
    }
}
