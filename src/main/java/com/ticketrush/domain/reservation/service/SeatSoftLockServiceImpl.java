package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.port.outbound.ReservationSeatPort;
import com.ticketrush.global.config.ReservationProperties;
import com.ticketrush.global.push.PushNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SeatSoftLockServiceImpl implements SeatSoftLockService {

    private static final String STATUS_SELECTING = "SELECTING";
    private static final String STATUS_RELEASED = "RELEASED";
    private static final String STATUS_HOLD = "HOLD";

    private final StringRedisTemplate redisTemplate;
    private final ReservationSeatPort reservationSeatPort;
    private final ReservationProperties reservationProperties;
    private final PushNotifier pushNotifier;

    @Override
    public SeatSoftLockAcquireResult acquire(Long userId, Long seatId, String requestId) {
        SeatContext context = seatContext(seatId);
        long ttlSeconds = normalizedTtlSeconds();
        String resolvedRequestId = resolveRequestId(requestId, userId, seatId);
        String expiresAt = Instant.now().plusSeconds(ttlSeconds).toString();
        String key = context.key();
        String encodedValue = encode(new LockValue(userId, resolvedRequestId, expiresAt));

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, encodedValue, ttlSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            LockValue current = decode(redisTemplate.opsForValue().get(key));
            if (current == null || !userId.equals(current.ownerUserId())) {
                throw new IllegalStateException("Seat is already selecting by another user. seatId=" + seatId);
            }
            redisTemplate.opsForValue().set(key, encodedValue, ttlSeconds, TimeUnit.SECONDS);
        }

        pushNotifier.sendSeatMapStatus(context.optionId(), seatId, STATUS_SELECTING, userId, expiresAt);
        return new SeatSoftLockAcquireResult(
                context.optionId(),
                seatId,
                userId,
                STATUS_SELECTING,
                resolvedRequestId,
                expiresAt,
                ttlSeconds
        );
    }

    @Override
    public SeatSoftLockReleaseResult release(Long userId, Long seatId) {
        SeatContext context = seatContext(seatId);
        String key = context.key();
        LockValue current = decode(redisTemplate.opsForValue().get(key));

        if (current == null) {
            return new SeatSoftLockReleaseResult(context.optionId(), seatId, STATUS_RELEASED, false);
        }
        if (!userId.equals(current.ownerUserId())) {
            throw new IllegalStateException("Seat soft lock owner mismatch. seatId=" + seatId);
        }

        redisTemplate.delete(key);
        pushNotifier.sendSeatMapStatus(context.optionId(), seatId, STATUS_RELEASED, null, null);
        return new SeatSoftLockReleaseResult(context.optionId(), seatId, STATUS_RELEASED, true);
    }

    @Override
    public void ensureHoldableByUser(Long userId, Long seatId) {
        SeatContext context = seatContext(seatId);
        LockValue current = decode(redisTemplate.opsForValue().get(context.key()));
        if (current != null && !userId.equals(current.ownerUserId())) {
            throw new IllegalStateException("Seat is selecting by another user. seatId=" + seatId);
        }
    }

    @Override
    public void promoteToHold(Long userId, Long seatId, LocalDateTime holdExpiresAt) {
        SeatContext context = seatContext(seatId);
        redisTemplate.delete(context.key());
        pushNotifier.sendSeatMapStatus(
                context.optionId(),
                seatId,
                STATUS_HOLD,
                userId,
                holdExpiresAt == null ? null : holdExpiresAt.toInstant(ZoneOffset.UTC).toString()
        );
    }

    private SeatContext seatContext(Long seatId) {
        Seat seat = reservationSeatPort.getSeat(seatId);
        Long optionId = seat.getConcertOption().getId();
        String key = reservationProperties.getSoftLockKeyPrefix() + optionId + ":" + seatId;
        return new SeatContext(optionId, key);
    }

    private long normalizedTtlSeconds() {
        return Math.max(1L, reservationProperties.getSoftLockTtlSeconds());
    }

    private String resolveRequestId(String requestId, Long userId, Long seatId) {
        if (StringUtils.hasText(requestId)) {
            return requestId.trim();
        }
        return "soft-lock-" + userId + "-" + seatId + "-" + System.currentTimeMillis();
    }

    private String encode(LockValue value) {
        return value.ownerUserId() + "|" + value.requestId() + "|" + value.expiresAt();
    }

    private LockValue decode(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String[] parts = rawValue.split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }
        try {
            Long ownerUserId = Long.parseLong(parts[0]);
            return new LockValue(ownerUserId, parts[1], parts[2]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record LockValue(Long ownerUserId, String requestId, String expiresAt) {
    }

    private record SeatContext(Long optionId, String key) {
    }
}
