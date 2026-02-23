package com.ticketrush.application.waitingqueue.port.outbound;

import java.util.List;

public interface WaitingQueueStore {

    JoinAndRankResult joinAndRank(String queueKey, String activeKey, String userId, long score, long maxQueueSize);

    boolean hasActiveUser(String activeKey);

    Long rank(String queueKey, String userId);

    List<String> activateUsers(String queueKey, String activeKeyPrefix, long ttlSeconds, long count);

    Long ttlSeconds(String key);

    record JoinAndRankResult(long resultCode, long rank) {
    }
}
