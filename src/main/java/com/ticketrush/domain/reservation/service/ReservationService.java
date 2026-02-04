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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ConcertService concertService;
    private final UserService userService;

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
}
