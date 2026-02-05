package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.service.UserService;
import com.ticketrush.interfaces.dto.ReservationRequest;
import com.ticketrush.interfaces.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ConcertService concertService;
    private final UserService userService;
    private final RedissonClient redissonClient;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        // 1. 유저 조회 (UserService 위임)
        User user = userService.getUser(request.userId());

        // 2. 좌석 조회 (ConcertService 위임)
        Seat seat = concertService.getSeat(request.seatId());

        // 3. 좌석 점유 시도
        seat.reserve();

        // 4. 예약 생성
        Reservation reservation = new Reservation(user, seat);
        reservationRepository.save(reservation);

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse createReservationWithPessimisticLock(ReservationRequest request) {
        // 1. 유저 조회 (UserService 위임)
        User user = userService.getUser(request.userId());

        // 2. 좌석 조회 (비관적 락 적용)
        Seat seat = concertService.getSeatWithPessimisticLock(request.seatId());

        // 3. 좌석 점유 시도
        seat.reserve();

        // 4. 예약 생성
        Reservation reservation = new Reservation(user, seat);
        reservationRepository.save(reservation);

        return ReservationResponse.from(reservation);
    }

    public ReservationResponse createReservationWithDistributedLock(ReservationRequest request) {
        String lockKey = "lock:seat:" + request.seatId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 시도 (최대 10초 대기, 락 획득 후 2초간 점유)
            boolean available = lock.tryLock(10, 2, TimeUnit.SECONDS);

            if (!available) {
                throw new RuntimeException("락 획득 실패: 잠시 후 다시 시도해주세요.");
            }

            // 2. 비즈니스 로직 실행
            return createReservation(request);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 3. 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}