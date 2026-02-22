package com.ticketrush.global.push;

import com.ticketrush.global.config.WaitingQueueProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketPushNotifierTest {

    @Test
    void subscribeQueue_shouldPersistSubscriptionToRedis() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        WebSocketPushNotifier notifier = new WebSocketPushNotifier(messagingTemplate, redisTemplate, waitingQueueProperties());

        String destination = notifier.subscribeQueue(100L, 1L);

        assertThat(destination).isEqualTo("/topic/waiting-queue/1/100");
        verify(zSetOperations).add(eq("ws:queue:subs:1"), eq("100"), anyDouble());
        verify(setOperations).add("ws:queue:concerts", "1");
    }

    @Test
    void getSubscribedQueueUsers_shouldReadFromRedis() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(zSetOperations.rangeByScore(eq("ws:queue:subs:1"), anyDouble(), anyDouble()))
                .thenReturn(Set.of("100", "101"));

        WebSocketPushNotifier notifier = new WebSocketPushNotifier(messagingTemplate, redisTemplate, waitingQueueProperties());

        Set<Long> users = notifier.getSubscribedQueueUsers(1L);

        assertThat(users).containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void sendQueueHeartbeat_shouldPublishToSubscribedUsers() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("ws:queue:concerts")).thenReturn(Set.of("1"));
        when(zSetOperations.rangeByScore(eq("ws:queue:subs:1"), anyDouble(), anyDouble()))
                .thenReturn(Set.of("100"));

        WebSocketPushNotifier notifier = new WebSocketPushNotifier(messagingTemplate, redisTemplate, waitingQueueProperties());

        notifier.sendQueueHeartbeat();

        verify(messagingTemplate).convertAndSend(eq("/topic/waiting-queue/1/100"), anyMap());
    }

    @Test
    void sendQueueActivated_shouldPublishToWaitingQueueTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        WebSocketPushNotifier notifier = new WebSocketPushNotifier(messagingTemplate, redisTemplate, waitingQueueProperties());

        notifier.sendQueueActivated(100L, 1L, Map.of("status", "ACTIVE"));

        verify(messagingTemplate).convertAndSend(eq("/topic/waiting-queue/1/100"), anyMap());
    }

    @Test
    void sendReservationStatus_shouldPublishToReservationTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        WebSocketPushNotifier notifier = new WebSocketPushNotifier(messagingTemplate, redisTemplate, waitingQueueProperties());

        notifier.sendReservationStatus(100L, 500L, "SUCCESS");

        verify(messagingTemplate).convertAndSend(eq("/topic/reservations/500/100"), anyMap());
    }

    private WaitingQueueProperties waitingQueueProperties() {
        WaitingQueueProperties properties = new WaitingQueueProperties();
        properties.setWsSubscriberZsetKeyPrefix("ws:queue:subs:");
        properties.setWsConcertIndexKey("ws:queue:concerts");
        properties.setWsSubscriberTtlSeconds(300);
        return properties;
    }
}
