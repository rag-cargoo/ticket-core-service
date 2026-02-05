package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.controller.ReservationController;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.interfaces.dto.ReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * <h2>동시성 테스트 - Step 4: Kafka 비동기 대기열</h2>
 * <p>
 * 예약 요청을 Kafka에 넣고, 별도 컨슈머가 비동기로 처리한 후 
 * Redis 상태가 SUCCESS로 변하는지 검증합니다.
 * </p>
 */
@SpringBootTest
class 동시성_테스트_4_비동기_대기열 {

    @Autowired
    private ReservationController reservationController;

    @Autowired
    private ReservationQueueService queueService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    private Long targetSeatId;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("asyncUser_" + System.currentTimeMillis()));
        targetUserId = user.getId();

        Seat seat = seatRepository.findAll().stream()
                .filter(s -> s.getStatus() == Seat.SeatStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        targetSeatId = seat.getId();
    }

    @Test
    @DisplayName("비동기 예약 흐름 검증: PENDING -> SUCCESS")
    void asyncReservationFlowTest() {
        // 1. 비동기 예약 요청 (v4)
        ReservationRequest request = new ReservationRequest(targetUserId, targetSeatId);
        reservationController.createPollingOptimisticReservation(request);

        // 2. Redis 상태가 SUCCESS가 될 때까지 대기 (최대 5초)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Map<String, String> statusResponse = reservationController.getReservationStatus(targetUserId, targetSeatId).getBody();
            assertThat(statusResponse.get("status")).isEqualTo("SUCCESS");
        });

        // 3. 최종 DB 좌석 상태 확인
        Seat seat = seatRepository.findById(targetSeatId).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(Seat.SeatStatus.RESERVED);
    }
}
