package com.ticketrush.application.waitingqueue.service;

import com.ticketrush.application.waitingqueue.model.WaitingQueueJoinCommand;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusQuery;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusResult;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusType;
import com.ticketrush.application.waitingqueue.port.outbound.WaitingQueueStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WaitingQueueServiceImpl implements WaitingQueueService {

    private final WaitingQueueStore waitingQueueStore;
    private final com.ticketrush.global.config.WaitingQueueProperties properties;
    private static final long JOIN_RESULT_ACTIVE = 1L;
    private static final long JOIN_RESULT_REJECTED = 2L;
    private static final long JOIN_RESULT_WAITING = 3L;
    private static final long JOIN_RESULT_UNKNOWN = 4L;

    @Override
    public WaitingQueueStatusResult join(WaitingQueueJoinCommand command) {
        String queueKey = properties.getQueueKeyPrefix() + command.getConcertId();
        String userIdStr = String.valueOf(command.getUserId());
        String activeKey = properties.getActiveKeyPrefix() + userIdStr;

        // Redis round trip을 줄이기 위해 join/throttling/rank 계산을 Lua 스크립트로 원자 처리한다.
        WaitingQueueStore.JoinAndRankResult scriptResult = waitingQueueStore.joinAndRank(
                queueKey,
                activeKey,
                userIdStr,
                System.currentTimeMillis(),
                properties.getMaxQueueSize()
        );
        if (scriptResult == null) {
            return getStatus(new WaitingQueueStatusQuery(command.getUserId(), command.getConcertId()));
        }

        long resultCode = scriptResult.resultCode();
        long rank = scriptResult.rank();

        if (resultCode == JOIN_RESULT_ACTIVE) {
            return buildResponse(command.getUserId(), command.getConcertId(), WaitingQueueStatusType.ACTIVE, 0L);
        }
        if (resultCode == JOIN_RESULT_REJECTED) {
            return buildResponse(command.getUserId(), command.getConcertId(), WaitingQueueStatusType.REJECTED, -1L);
        }
        if (resultCode == JOIN_RESULT_WAITING) {
            return buildResponse(command.getUserId(), command.getConcertId(), WaitingQueueStatusType.WAITING, rank > 0 ? rank : 1L);
        }

        return getStatus(new WaitingQueueStatusQuery(command.getUserId(), command.getConcertId()));
    }

    @Override
    public WaitingQueueStatusResult getStatus(WaitingQueueStatusQuery query) {
        String userIdStr = String.valueOf(query.getUserId());

        // 1. 활성 상태 확인
        if (waitingQueueStore.hasActiveUser(properties.getActiveKeyPrefix() + userIdStr)) {
            return buildResponse(query.getUserId(), query.getConcertId(), WaitingQueueStatusType.ACTIVE, 0L);
        }

        // 2. 대기 순번 조회
        String queueKey = properties.getQueueKeyPrefix() + query.getConcertId();
        Long rank = waitingQueueStore.rank(queueKey, userIdStr);

        return buildResponse(
                query.getUserId(),
                query.getConcertId(),
                rank != null ? WaitingQueueStatusType.WAITING : WaitingQueueStatusType.NONE,
                rank != null ? rank + 1 : -1L // 0-based rank이므로 +1
        );
    }

    @Override
    public List<Long> activateUsers(Long concertId, long count) {
        if (count <= 0) {
            return List.of();
        }

        String queueKey = properties.getQueueKeyPrefix() + concertId;
        long activeTtlSeconds = TimeUnit.MINUTES.toSeconds(properties.getActiveTtlMinutes());
        List<String> rawActivatedUsers = waitingQueueStore.activateUsers(
                queueKey,
                properties.getActiveKeyPrefix(),
                activeTtlSeconds,
                count
        );

        if (rawActivatedUsers == null || rawActivatedUsers.isEmpty()) {
            return List.of();
        }

        List<Long> activatedUsers = new ArrayList<>(rawActivatedUsers.size());
        for (String rawUserId : rawActivatedUsers) {
            Long userId = parseLong(rawUserId);
            if (userId != null) {
                activatedUsers.add(userId);
            }
        }
        return activatedUsers;
    }

    @Override
    public Long getActiveTtlSeconds(Long userId) {
        String activeKey = properties.getActiveKeyPrefix() + userId;
        Long ttl = waitingQueueStore.ttlSeconds(activeKey);
        return (ttl != null && ttl > 0) ? ttl : 0L;
    }

    private WaitingQueueStatusResult buildResponse(Long userId, Long concertId, WaitingQueueStatusType status, Long rank) {
        return WaitingQueueStatusResult.builder()
                .userId(userId)
                .concertId(concertId)
                .status(status)
                .rank(rank)
                .build();
    }

    private Long parseLong(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(rawValue));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
