package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.controller.ReservationController;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.api.dto.ReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

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

    private static final String TEST_RUN_ID = UUID.randomUUID().toString().replace("-", "");
    private static final String TEST_TOPIC = "ticket-reservation-events-test-" + TEST_RUN_ID;
    private static final String TEST_GROUP = "ticket-group-test-" + TEST_RUN_ID;

    @DynamicPropertySource
    static void overrideKafkaProps(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.topic.reservation", () -> TEST_TOPIC);
        registry.add("spring.kafka.consumer.group-id", () -> TEST_GROUP);
    }

    @Autowired
    private ReservationController reservationController;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConcertService concertService;

    private Long targetSeatId;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.currentTimeMillis());
        User user = userRepository.save(new User("asyncUser_" + suffix));
        targetUserId = user.getId();

        Concert concert = concertService.createConcert(
                "testConcert_async_" + suffix,
                "testArtist_async_" + suffix,
                "testAgency_async_" + suffix
        );
        ConcertOption option = concertService.addOption(concert.getId(), LocalDateTime.now().plusDays(1));
        concertService.createSeats(option.getId(), 1);

        Seat seat = seatRepository.findByConcertOptionIdAndStatus(option.getId(), Seat.SeatStatus.AVAILABLE).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seat setup failed"));
        targetSeatId = seat.getId();
    }

    @Test
    @DisplayName("비동기 예약 흐름 검증: PENDING -> SUCCESS")
    void asyncReservationFlowTest() {
        // 1. 비동기 예약 요청 (v4)
        ReservationRequest request = new ReservationRequest(targetUserId, targetSeatId);
        reservationController.createPollingOptimisticReservation(request);

        // 2. Redis 상태가 SUCCESS가 될 때까지 대기 (CI/부하 환경 고려, 최대 15초)
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Map<String, String> statusResponse = reservationController.getReservationStatus(targetUserId, targetSeatId).getBody();
            String status = statusResponse.get("status");
            assertThat(status).isIn("PENDING", "PROCESSING", "SUCCESS");
            if ("SUCCESS".equals(status)) {
                assertThat(status).isEqualTo("SUCCESS");
            } else {
                throw new AssertionError("still waiting: status=" + status);
            }
        });

        // 3. 최종 DB 좌석 상태 확인
        Seat seat = seatRepository.findById(targetSeatId).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(Seat.SeatStatus.RESERVED);
    }
}
