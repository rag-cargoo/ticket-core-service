package com.ticketrush.domain.reservation.controller;

import com.ticketrush.domain.reservation.service.ReservationService;
import com.ticketrush.infrastructure.lock.RedissonLockFacade;
import com.ticketrush.interfaces.dto.ReservationRequest;
import com.ticketrush.interfaces.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final RedissonLockFacade redissonLockFacade;

    /**
     * [v1] 낙관적 락 버전
     */
    @PostMapping("/v1/optimistic")
    public ResponseEntity<ReservationResponse> createOptimisticReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createReservation(request));
    }

    /**
     * [v2] 비관적 락 버전
     */
    @PostMapping("/v2/pessimistic")
    public ResponseEntity<ReservationResponse> createPessimisticReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createReservationWithPessimisticLock(request));
    }

    /**
     * [v3] Redis 분산 락 버전
     */
    @PostMapping("/v3/distributed-lock")
    public ResponseEntity<ReservationResponse> createDistributedLockReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(redissonLockFacade.createReservation(request));
    }

    /**
     * [Read] 유저별 예약 목록 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(@PathVariable Long userId) {
        return ResponseEntity.ok(reservationService.getReservationsByUserId(userId));
    }

    /**
     * [Delete] 예약 취소
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }
}