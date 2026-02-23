package com.ticketrush.global.lock;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationResult;
import com.ticketrush.application.reservation.port.inbound.DistributedReservationUseCase;
import com.ticketrush.application.reservation.port.inbound.ReservationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockFacade implements DistributedReservationUseCase {

    private final RedissonClient redissonClient;
    private final ReservationUseCase reservationUseCase;

    // 비즈니스 정책: 사용자는 최대 10초까지 대기할 수 있음 (상수로 관리하거나 설정에서 주입)
    private static final long WAIT_TIME = 10L;

    @Override
    public ReservationResult createReservation(ReservationCreateCommand command) {
        String lockKey = "lock:seat:" + command.getSeatId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // [자율적 설계 적용]
            // waitTime: 정책에 따른 대기 (10초)
            // leaseTime: -1 (Redisson Watchdog 활성화. 로직 실행 시간에 맞춰 스스로 연장)
            boolean available = lock.tryLock(WAIT_TIME, -1, TimeUnit.SECONDS);

            if (!available) {
                log.warn("락 획득 실패 - SeatId: {}, UserId: {}", command.getSeatId(), command.getUserId());
                throw new RuntimeException("현재 예약 요청이 많습니다. 잠시 후 다시 시도해주세요.");
            }

            return reservationUseCase.createReservation(command);

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
