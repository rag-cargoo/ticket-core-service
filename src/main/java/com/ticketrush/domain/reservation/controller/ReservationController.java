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
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.ok(response);
    }

    /**
     * [v1] 낙관적 락(Optimistic Lock) 버전
     */
    @PostMapping("/v1/optimistic")
    public ResponseEntity<ReservationResponse> createOptimisticReservation(@RequestBody ReservationRequest request) {
        return createReservation(request);
    }

    /**
     * [v2] 비관적 락(Pessimistic Lock) 버전
     */
    @PostMapping("/v2/pessimistic")
    public ResponseEntity<ReservationResponse> createPessimisticReservation(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservationWithPessimisticLock(request);
        return ResponseEntity.ok(response);
    }

    /**
     * [v3] Redis 분산 락 버전
     */
    @PostMapping("/v3/distributed-lock")
    public ResponseEntity<ReservationResponse> createDistributedLockReservation(@RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservationWithDistributedLock(request);
        return ResponseEntity.ok(response);
    }
}