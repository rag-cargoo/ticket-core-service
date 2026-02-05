package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.global.lock.RedissonLockFacade;
import com.ticketrush.interfaces.dto.ReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class 동시성_테스트_3_분산_락 {

    @Autowired
    private RedissonLockFacade redissonLockFacade;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    private Long targetSeatId;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("testUser_Redis_" + System.currentTimeMillis()));
        targetUserId = user.getId();

        Seat seat = seatRepository.findAll().stream()
                .filter(s -> s.getStatus() == Seat.SeatStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        targetSeatId = seat.getId();
    }

    @Test
    @DisplayName("Redis 분산 락 동작 확인: 30명 동시 예약 시 1명만 성공 (Facade 적용 버전)")
    void concurrentTest() throws InterruptedException {
        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // Facade를 통한 분산 락 예약 호출
                    redissonLockFacade.createReservation(new ReservationRequest(targetUserId, targetSeatId));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("예약 실패: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long actualReservationCount = reservationRepository.findBySeatId(targetSeatId).size();
        
        System.out.println("================ 실험 결과 (Redis 분산 락 - Facade) ================");
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("해당 좌석 예약 건수: " + actualReservationCount);
        System.out.println("===============================================================");

        assertThat(actualReservationCount).isEqualTo(1);
    }
}