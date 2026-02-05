package com.ticketrush.infrastructure.lock;

import com.ticketrush.domain.reservation.service.ReservationService;
import com.ticketrush.interfaces.dto.ReservationRequest;
import com.ticketrush.interfaces.dto.ReservationResponse;
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

    public ReservationResponse createReservation(ReservationRequest request) {
        String lockKey = "lock:seat:" + request.seatId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 시도 (최대 10초 대기, 락 획득 후 2초간 점유)
            boolean available = lock.tryLock(10, 2, TimeUnit.SECONDS);

            if (!available) {
                throw new RuntimeException("락 획득 실패: 잠시 후 다시 시도해주세요.");
            }

            // 2. 비즈니스 로직 실행 (외부 Service 호출을 통해 트랜잭션 완벽 보장)
            // Service의 트랜잭션이 커밋된 후에 이 메서드가 리턴됨.
            return reservationService.createReservation(request);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 3. 락 해제
            // 트랜잭션이 완전히 끝난 후(Service 리턴 후)에 락이 풀림
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
