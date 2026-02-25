package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationListItemResult;
import com.ticketrush.application.reservation.model.ReservationResult;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.port.outbound.ReservationSeatPort;
import com.ticketrush.domain.reservation.port.outbound.ReservationUserPort;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.application.concert.port.outbound.ConcertReadCacheEvictPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatPort reservationSeatPort;
    private final ReservationUserPort reservationUserPort;
    private final ConcertReadCacheEvictPort concertReadCacheEvictor;

    /**
     * [v1] 낙관적 락(Optimistic Lock)을 사용한 예약 생성
     */
    @Transactional
    public ReservationResult createReservation(ReservationCreateCommand command) {
        // 1. 유저 조회 (Reservation boundary port 위임)
        User user = reservationUserPort.getUser(command.getUserId());

        // 2. 좌석 조회 (Reservation boundary port 위임)
        Seat seat = reservationSeatPort.getSeat(command.getSeatId());

        // 3. 좌석 점유 시도
        seat.reserve();

        // 4. 예약 생성
        Reservation reservation = new Reservation(user, seat);
        reservationRepository.save(reservation);
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(seat.getConcertOption().getId());

        return ReservationResult.from(reservation);
    }

    /**
     * [v2] 비관적 락(Pessimistic Lock)을 사용한 예약 생성
     */
    @Transactional
    public ReservationResult createReservationWithPessimisticLock(ReservationCreateCommand command) {
        // 1. 유저 조회
        User user = reservationUserPort.getUser(command.getUserId());

        // 2. 좌석 조회 (비관적 락 적용)
        Seat seat = reservationSeatPort.getSeatWithPessimisticLock(command.getSeatId());

        // 3. 좌석 점유 시도
        seat.reserve();

        // 4. 예약 생성
        Reservation reservation = new Reservation(user, seat);
        reservationRepository.save(reservation);
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(seat.getConcertOption().getId());

        return ReservationResult.from(reservation);
    }

    /**
     * [Read] 유저별 예약 내역 조회
     */
    @Transactional(readOnly = true)
    public List<ReservationResult> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(ReservationResult::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationListItemResult> getReservationsByUserId(
            Long userId,
            Long concertId,
            Long optionId,
            List<Reservation.ReservationStatus> statuses
    ) {
        List<Reservation.ReservationStatus> normalizedStatuses = statuses == null
                ? List.of()
                : statuses.stream()
                .filter(status -> status != null)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new))
                .stream()
                .toList();
        boolean statusesEmpty = normalizedStatuses.isEmpty();
        List<Reservation.ReservationStatus> queryStatuses = statusesEmpty
                ? List.of(Reservation.ReservationStatus.PENDING)
                : normalizedStatuses;

        return reservationRepository.findByUserIdWithFilters(
                        userId,
                        concertId,
                        optionId,
                        statusesEmpty,
                        queryStatuses
                ).stream()
                .map(ReservationListItemResult::from)
                .toList();
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
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(reservation.getSeat().getConcertOption().getId());
        
        reservationRepository.delete(reservation);
    }
}
