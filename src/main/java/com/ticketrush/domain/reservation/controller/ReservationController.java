package com.ticketrush.domain.reservation.controller;

import com.ticketrush.domain.reservation.service.ReservationService;
import com.ticketrush.domain.reservation.service.ReservationQueueService;
import com.ticketrush.domain.reservation.event.ReservationEvent;
import com.ticketrush.infrastructure.lock.RedissonLockFacade;
import com.ticketrush.infrastructure.messaging.KafkaReservationProducer;
import com.ticketrush.infrastructure.sse.SseEmitterManager;
import com.ticketrush.interfaces.dto.ReservationRequest;
import com.ticketrush.interfaces.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final RedissonLockFacade redissonLockFacade;
    private final KafkaReservationProducer kafkaProducer;
    private final ReservationQueueService queueService;
    private final SseEmitterManager sseManager;

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
     * [v4-opt] 비동기 대기열 + 낙관적 락
     */
    @PostMapping("/v4-opt/queue-polling")
    public ResponseEntity<Map<String, String>> createPollingOptimisticReservation(@RequestBody ReservationRequest request) {
        return enqueue(request, ReservationEvent.LockType.OPTIMISTIC);
    }

    /**
     * [v4-pes] 비동기 대기열 + 비관적 락
     */
    @PostMapping("/v4-pes/queue-polling")
    public ResponseEntity<Map<String, String>> createPollingPessimisticReservation(@RequestBody ReservationRequest request) {
        return enqueue(request, ReservationEvent.LockType.PESSIMISTIC);
    }

    /**
     * [v4/status] 비동기 예약 상태 조회 (Polling용)
     */
    @GetMapping("/v4/status")
    public ResponseEntity<Map<String, String>> getReservationStatus(
            @RequestParam Long userId, 
            @RequestParam Long seatId) {
        String status = queueService.getStatus(userId, seatId);
        return ResponseEntity.ok(Map.of("status", status != null ? status : "NOT_FOUND"));
    }

    /**
     * [v5] SSE 실시간 구독
     */
    @GetMapping(value = "/v5/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam Long userId,
            @RequestParam Long seatId) {
        return sseManager.subscribe(userId, seatId);
    }

    /**
     * [v5-opt] 비동기 대기열 + SSE + 낙관적 락
     */
    @PostMapping("/v5-opt/queue-sse")
    public ResponseEntity<Map<String, String>> createSseOptimisticReservation(@RequestBody ReservationRequest request) {
        return enqueue(request, ReservationEvent.LockType.OPTIMISTIC);
    }

    private ResponseEntity<Map<String, String>> enqueue(ReservationRequest request, ReservationEvent.LockType lockType) {
        queueService.setStatus(request.userId(), request.seatId(), "PENDING");
        kafkaProducer.send(ReservationEvent.of(request.userId(), request.seatId(), lockType));
        return ResponseEntity.accepted().body(Map.of(
            "message", "Reservation request enqueued",
            "strategy", lockType.name()
        ));
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