package com.ticketrush.global.lock;

import com.ticketrush.domain.reservation.service.ReservationService;
import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockFacade {

    private final RedissonClient redissonClient;
    private final ReservationService reservationService;

    // 비즈니스 정책: 사용자는 최대 10초까지 대기할 수 있음 (상수로 관리하거나 설정에서 주입)
    private static final long WAIT_TIME = 10L;

    public ReservationResponse createReservation(ReservationRequest request) {
        String lockKey = "lock:seat:" + request.getSeatId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // [자율적 설계 적용]
            // waitTime: 정책에 따른 대기 (10초)
            // leaseTime: -1 (Redisson Watchdog 활성화. 로직 실행 시간에 맞춰 스스로 연장)
            boolean available = lock.tryLock(WAIT_TIME, -1, TimeUnit.SECONDS);

            if (!available) {
                log.warn("락 획득 실패 - SeatId: {}, UserId: {}", request.getSeatId(), request.getUserId());
                throw new RuntimeException("현재 예약 요청이 많습니다. 잠시 후 다시 시도해주세요.");
            }

            return reservationService.createReservation(request);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("시스템 오류가 발생했습니다.", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}