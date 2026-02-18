package com.ticketrush.api.controller;

import com.ticketrush.domain.reservation.service.ReservationService;
import com.ticketrush.domain.reservation.service.ReservationQueueService;
import com.ticketrush.domain.reservation.service.AbuseAuditService;
import com.ticketrush.domain.reservation.service.ReservationLifecycleService;
import com.ticketrush.domain.reservation.event.ReservationEvent;
import com.ticketrush.domain.reservation.entity.AbuseAuditLog;
import com.ticketrush.global.lock.RedissonLockFacade;
import com.ticketrush.global.messaging.KafkaReservationProducer;
import com.ticketrush.global.sse.SsePushNotifier;
import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.ReservationResponse;
import com.ticketrush.api.dto.reservation.AuthenticatedHoldRequest;
import com.ticketrush.api.dto.reservation.AbuseAuditResponse;
import com.ticketrush.api.dto.reservation.ReservationLifecycleResponse;
import com.ticketrush.api.dto.reservation.ReservationStateRequest;
import com.ticketrush.domain.auth.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
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
    private final ReservationLifecycleService reservationLifecycleService;
    private final AbuseAuditService abuseAuditService;
    private final SsePushNotifier ssePushNotifier;

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
        return ssePushNotifier.subscribeReservation(userId, seatId);
    }

    /**
     * [v5-opt] 비동기 대기열 + SSE + 낙관적 락
     */
    @PostMapping("/v5-opt/queue-sse")
    public ResponseEntity<Map<String, String>> createSseOptimisticReservation(@RequestBody ReservationRequest request) {
        return enqueue(request, ReservationEvent.LockType.OPTIMISTIC);
    }

    /**
     * [v6] Step 9 - 좌석 홀드 생성
     */
    @PostMapping("/v6/holds")
    public ResponseEntity<ReservationLifecycleResponse> createHold(@RequestBody ReservationRequest request) {
        return ResponseEntity.status(201).body(reservationLifecycleService.createHold(request));
    }

    /**
     * [v6] Step 9 - 결제 진행 상태 전이 (HOLD -> PAYING)
     */
    @PostMapping("/v6/{reservationId}/paying")
    public ResponseEntity<ReservationLifecycleResponse> startPaying(
            @PathVariable Long reservationId,
            @RequestBody ReservationStateRequest request) {
        return ResponseEntity.ok(reservationLifecycleService.startPaying(reservationId, request.getUserId()));
    }

    /**
     * [v6] Step 9 - 결제 확정 상태 전이 (PAYING -> CONFIRMED)
     */
    @PostMapping("/v6/{reservationId}/confirm")
    public ResponseEntity<ReservationLifecycleResponse> confirm(
            @PathVariable Long reservationId,
            @RequestBody ReservationStateRequest request) {
        return ResponseEntity.ok(reservationLifecycleService.confirm(reservationId, request.getUserId()));
    }

    /**
     * [v6] Step 10 - 예약 취소 상태 전이 (CONFIRMED -> CANCELLED)
     */
    @PostMapping("/v6/{reservationId}/cancel")
    public ResponseEntity<ReservationLifecycleResponse> cancel(
            @PathVariable Long reservationId,
            @RequestBody ReservationStateRequest request) {
        return ResponseEntity.ok(reservationLifecycleService.cancel(reservationId, request.getUserId()));
    }

    /**
     * [v6] Step 10 - 환불 완료 상태 전이 (CANCELLED -> REFUNDED)
     */
    @PostMapping("/v6/{reservationId}/refund")
    public ResponseEntity<ReservationLifecycleResponse> refund(
            @PathVariable Long reservationId,
            @RequestBody ReservationStateRequest request) {
        return ResponseEntity.ok(reservationLifecycleService.refund(reservationId, request.getUserId()));
    }

    /**
     * [v6] Step 9 - 예약 상태 조회
     */
    @GetMapping("/v6/{reservationId}")
    public ResponseEntity<ReservationLifecycleResponse> getReservation(
            @PathVariable Long reservationId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(reservationLifecycleService.getReservation(reservationId, userId));
    }

    /**
     * [v6] Step 12 - 부정사용/감사 추적 로그 조회
     */
    @GetMapping("/v6/audit/abuse")
    public ResponseEntity<List<AbuseAuditResponse>> getAbuseAudits(
            @RequestParam(required = false) AbuseAuditLog.AuditAction action,
            @RequestParam(required = false) AbuseAuditLog.AuditResult result,
            @RequestParam(required = false) AbuseAuditLog.AuditReason reason,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long concertId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toAt,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(
                abuseAuditService.getAuditLogs(action, result, reason, userId, concertId, fromAt, toAt, limit)
                        .stream()
                        .map(AbuseAuditResponse::from)
                        .toList()
        );
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 HOLD 생성
     */
    @PostMapping("/v7/holds")
    public ResponseEntity<ReservationLifecycleResponse> createHoldV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestBody AuthenticatedHoldRequest request
    ) {
        return ResponseEntity.status(201).body(
                reservationLifecycleService.createHold(request.toReservationRequest(requiredUserId(principal)))
        );
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 PAYING 전이
     */
    @PostMapping("/v7/{reservationId}/paying")
    public ResponseEntity<ReservationLifecycleResponse> startPayingV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(reservationLifecycleService.startPaying(reservationId, requiredUserId(principal)));
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 CONFIRMED 전이
     */
    @PostMapping("/v7/{reservationId}/confirm")
    public ResponseEntity<ReservationLifecycleResponse> confirmV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(reservationLifecycleService.confirm(reservationId, requiredUserId(principal)));
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 CANCELLED 전이
     */
    @PostMapping("/v7/{reservationId}/cancel")
    public ResponseEntity<ReservationLifecycleResponse> cancelV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(reservationLifecycleService.cancel(reservationId, requiredUserId(principal)));
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 REFUNDED 전이
     */
    @PostMapping("/v7/{reservationId}/refund")
    public ResponseEntity<ReservationLifecycleResponse> refundV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(reservationLifecycleService.refund(reservationId, requiredUserId(principal)));
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 예약 상태 조회
     */
    @GetMapping("/v7/{reservationId}")
    public ResponseEntity<ReservationLifecycleResponse> getReservationV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(reservationLifecycleService.getReservation(reservationId, requiredUserId(principal)));
    }

    /**
     * [v7] Auth Track A2 - 본인 예약 목록 조회
     */
    @GetMapping("/v7/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservationsV7(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ResponseEntity.ok(reservationService.getReservationsByUserId(requiredUserId(principal)));
    }

    /**
     * [v7] Auth Track A2 - 부정사용 감사 로그 조회 (관리자 권한 전용)
     */
    @GetMapping("/v7/audit/abuse")
    public ResponseEntity<List<AbuseAuditResponse>> getAbuseAuditsV7(
            @RequestParam(required = false) AbuseAuditLog.AuditAction action,
            @RequestParam(required = false) AbuseAuditLog.AuditResult result,
            @RequestParam(required = false) AbuseAuditLog.AuditReason reason,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long concertId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toAt,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(
                abuseAuditService.getAuditLogs(action, result, reason, userId, concertId, fromAt, toAt, limit)
                        .stream()
                        .map(AbuseAuditResponse::from)
                        .toList()
        );
    }

    private ResponseEntity<Map<String, String>> enqueue(ReservationRequest request, ReservationEvent.LockType lockType) {
        queueService.setStatus(request.getUserId(), request.getSeatId(), "PENDING");
        kafkaProducer.send(ReservationEvent.of(request.getUserId(), request.getSeatId(), lockType));
        return ResponseEntity.accepted().body(Map.of(
            "message", "Reservation request enqueued",
            "strategy", lockType.name()
        ));
    }

    private Long requiredUserId(AuthUserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("authenticated user is required");
        }
        return principal.getUserId();
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
