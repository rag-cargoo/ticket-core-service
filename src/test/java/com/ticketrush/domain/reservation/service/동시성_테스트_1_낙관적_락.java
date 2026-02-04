package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
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

/**
 * <h2>동시성 테스트 - Step 1: 낙관적 락 (Optimistic Lock)</h2>
 * <p>
 * 본 테스트는 좌석 예약 시 발생할 수 있는 Race Condition 상황에서
 * JPA의 {@code @Version}을 이용한 낙관적 락이 어떻게 데이터 정합성을 유지하는지 확인합니다.
 * </p>
 *
 * <h3>검증 지표</h3>
 * <ul>
 *     <li><b>총 요청 수</b>: 30건</li>
 *     <li><b>기대 성공 수</b>: 1건</li>
 *     <li><b>기대 실패 수</b>: 29건 (Optimistic Lock 충돌)</li>
 * </ul>
 */
@SpringBootTest
class 동시성_테스트_1_낙관적_락 {

    @Autowired
    private ReservationService reservationService;

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
        User user = userRepository.save(new User("testUser_" + System.currentTimeMillis()));
        targetUserId = user.getId();

        Seat seat = seatRepository.findAll().stream()
                .filter(s -> s.getStatus() == Seat.SeatStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        targetSeatId = seat.getId();
    }

    @Test
    @DisplayName("낙관적 락 동작 확인: 30명 동시 예약 시 1명만 성공")
    void concurrentTest() throws InterruptedException {
        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    reservationService.createReservation(new ReservationRequest(targetUserId, targetSeatId));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // ObjectOptimisticLockingFailureException 발생 예상
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long actualReservationCount = reservationRepository.count();
        
        System.out.println("================ 실험 결과 ================");
        System.out.println("성공 유저 수: " + successCount.get());
        System.out.println("실패 유저 수: " + failCount.get());
        System.out.println("DB에 저장된 예약 건수: " + actualReservationCount);
        System.out.println("=========================================");

        assertThat(actualReservationCount).isEqualTo(1);
    }
}