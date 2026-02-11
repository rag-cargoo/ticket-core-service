package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.api.dto.ReservationRequest;
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
 * <h2>동시성 테스트 - Step 2: 비관적 락 (Pessimistic Lock)</h2>
 * <p>
 * DB의 <b>SELECT ... FOR UPDATE</b> 쿼리를 사용하여
 * 동시 접근 시 줄을 세우고(Wait), 데이터 정합성을 강력하게 보장하는지 검증합니다.
 * </p>
 *
 * <h3>검증 지표</h3>
 * <ul>
 *     <li><b>총 요청 수</b>: 30건</li>
 *     <li><b>기대 성공 수</b>: 1건</li>
 *     <li><b>기대 실패 수</b>: 29건 (이미 예약됨 예외)</li>
 *     <li><b>차이점</b>: 낙관적 락은 충돌 시 바로 에러가 나지만, 비관적 락은 앞선 트랜잭션이 끝날 때까지 대기했다가 처리됨.</li>
 * </ul>
 */
@SpringBootTest
class 동시성_테스트_2_비관적_락 {

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
        User user = userRepository.save(new User("testUser_Pessimistic_" + System.currentTimeMillis()));
        targetUserId = user.getId();

        Seat seat = seatRepository.findAll().stream()
                .filter(s -> s.getStatus() == Seat.SeatStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        targetSeatId = seat.getId();
    }

    @Test
    @DisplayName("비관적 락 동작 확인: 30명 동시 예약 시 순차적으로 처리되어 1명만 성공")
    void concurrentTest() throws InterruptedException {
        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 비관적 락 메서드 호출
                    reservationService.createReservationWithPessimisticLock(new ReservationRequest(targetUserId, targetSeatId));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 대기하다가 들어왔지만 이미 예약된 상태라면 실패 처리
                    System.out.println("예약 실패: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long actualReservationCount = reservationRepository.findBySeatId(targetSeatId).size();
        
        System.out.println("================ 실험 결과 (비관적 락) ================");
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("해당 좌석 예약 건수: " + actualReservationCount);
        System.out.println("====================================================");

        assertThat(actualReservationCount).isEqualTo(1);
    }
}
