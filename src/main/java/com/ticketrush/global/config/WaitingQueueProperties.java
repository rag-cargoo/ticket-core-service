package com.ticketrush.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.waiting-queue")
public class WaitingQueueProperties {
    private long maxQueueSize;
    private long activeTtlMinutes;
    private long activationBatchSize;
    private long activationConcertId;
    private long activationDelayMillis;
    private long sseTimeoutMillis;
    private long sseHeartbeatDelayMillis;
    private String queueKeyPrefix;
    private String activeKeyPrefix;
    private long wsSubscriberTtlSeconds = 90;
    private String wsSubscriberZsetKeyPrefix = "ws:queue:subs:";
    private String wsConcertIndexKey = "ws:queue:concerts";
}
