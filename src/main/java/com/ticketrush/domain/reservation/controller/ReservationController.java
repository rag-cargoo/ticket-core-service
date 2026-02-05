package com.ticketrush.domain.reservation.controller;

import com.ticketrush.domain.reservation.service.ReservationService;
import com.ticketrush.interfaces.dto.ReservationRequest;
import com.ticketrush.interfaces.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * [v1] 낙관적 락(Optimistic Lock)을 사용한 예약 생성
     */
    @PostMapping("/v1/reservations/optimistic")
    public ResponseEntity<ReservationResponse> createOptimisticReservation(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.ok(response);
    }

    /**
     * [v2] 비관적 락(Pessimistic Lock)을 사용한 예약 생성
     */
    @PostMapping("/v2/reservations/pessimistic")
    public ResponseEntity<ReservationResponse> createPessimisticReservation(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservationWithPessimisticLock(request);
        return ResponseEntity.ok(response);
    }
}
