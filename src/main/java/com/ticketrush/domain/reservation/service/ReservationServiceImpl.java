package com.ticketrush.domain.reservation.service;

import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.service.ConcertService;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.service.UserService;
import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ConcertService concertService;
    private final UserService userService;

    /**
     * [v1] 낙관적 락(Optimistic Lock)을 사용한 예약 생성
     */
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        // 1. 유저 조회 (UserService 위임)
        User user = userService.getUser(request.getUserId());

        // 2. 좌석 조회 (ConcertService 위임)
        Seat seat = concertService.getSeat(request.getSeatId());

        // 3. 좌석 점유 시도
        seat.reserve();

        // 4. 예약 생성
        Reservation reservation = new Reservation(user, seat);
        reservationRepository.save(reservation);

        return ReservationResponse.from(reservation);
    }

    /**
     * [v2] 비관적 락(Pessimistic Lock)을 사용한 예약 생성
     */
    @Transactional
    public ReservationResponse createReservationWithPessimisticLock(ReservationRequest request) {
        // 1. 유저 조회
        User user = userService.getUser(request.getUserId());

        // 2. 좌석 조회 (비관적 락 적용)
        Seat seat = concertService.getSeatWithPessimisticLock(request.getSeatId());

        // 3. 좌석 점유 시도
        seat.reserve();

        // 4. 예약 생성
        Reservation reservation = new Reservation(user, seat);
        reservationRepository.save(reservation);

        return ReservationResponse.from(reservation);
    }

    /**
     * [Read] 유저별 예약 내역 조회
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * [Delete] 예약 취소 (삭제)
     */
    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        
        // 좌석 상태 원복 (AVAILABLE)
        reservation.getSeat().cancel();
        
        reservationRepository.delete(reservation);
    }
}
