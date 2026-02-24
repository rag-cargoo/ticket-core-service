package com.ticketrush.api.controller;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationLifecycleResult;
import com.ticketrush.application.reservation.model.ReservationQueueLockType;
import com.ticketrush.application.reservation.model.ReservationResult;
import com.ticketrush.application.reservation.model.AbuseAuditActionType;
import com.ticketrush.application.reservation.model.AbuseAuditResultType;
import com.ticketrush.application.reservation.model.AbuseAuditReasonType;
import com.ticketrush.application.reservation.model.AdminRefundAuditResultType;
import com.ticketrush.application.realtime.port.inbound.RealtimeSubscriptionUseCase;
import com.ticketrush.application.reservation.port.inbound.AbuseAuditUseCase;
import com.ticketrush.application.reservation.port.inbound.AdminRefundAuditUseCase;
import com.ticketrush.application.reservation.port.inbound.DistributedReservationUseCase;
import com.ticketrush.application.reservation.port.inbound.ReservationLifecycleUseCase;
import com.ticketrush.application.reservation.port.inbound.ReservationQueueOrchestrationUseCase;
import com.ticketrush.application.reservation.port.inbound.ReservationUseCase;
import com.ticketrush.application.reservation.port.inbound.SeatSoftLockUseCase;
import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.ReservationResponse;
import com.ticketrush.api.dto.reservation.AuthenticatedHoldRequest;
import com.ticketrush.api.dto.reservation.AdminRefundAuditResponse;
import com.ticketrush.api.dto.reservation.AbuseAuditResponse;
import com.ticketrush.api.dto.reservation.ReservationLifecycleResponse;
import com.ticketrush.api.dto.reservation.SeatSoftLockAcquireResponse;
import com.ticketrush.api.dto.reservation.SeatSoftLockReleaseResponse;
import com.ticketrush.api.dto.reservation.SeatSoftLockRequest;
import com.ticketrush.api.dto.reservation.ReservationStateRequest;
import com.ticketrush.application.auth.model.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationUseCase reservationUseCase;
    private final DistributedReservationUseCase distributedReservationUseCase;
    private final RealtimeSubscriptionUseCase realtimeSubscriptionUseCase;
    private final ReservationQueueOrchestrationUseCase reservationQueueOrchestrationUseCase;
    private final ReservationLifecycleUseCase reservationLifecycleUseCase;
    private final SeatSoftLockUseCase seatSoftLockUseCase;
    private final AbuseAuditUseCase abuseAuditUseCase;
    private final AdminRefundAuditUseCase adminRefundAuditUseCase;

    /**
     * [v1] 낙관적 락 버전
     */
    @PostMapping("/v1/optimistic")
    public ResponseEntity<ReservationResponse> createOptimisticReservation(@RequestBody ReservationRequest request) {
        ReservationResult result = reservationUseCase.createReservation(toCreateCommand(request));
        return ResponseEntity.ok(ReservationResponse.from(result));
    }

    /**
     * [v2] 비관적 락 버전
     */
    @PostMapping("/v2/pessimistic")
    public ResponseEntity<ReservationResponse> createPessimisticReservation(@RequestBody ReservationRequest request) {
        ReservationResult result = reservationUseCase.createReservationWithPessimisticLock(toCreateCommand(request));
        return ResponseEntity.ok(ReservationResponse.from(result));
    }

    /**
     * [v3] Redis 분산 락 버전
     */
    @PostMapping("/v3/distributed-lock")
    public ResponseEntity<ReservationResponse> createDistributedLockReservation(@RequestBody ReservationRequest request) {
        ReservationResult result = distributedReservationUseCase.createReservation(toCreateCommand(request));
        return ResponseEntity.ok(ReservationResponse.from(result));
    }

    /**
     * [v4-opt] 비동기 대기열 + 낙관적 락
     */
    @PostMapping("/v4-opt/queue-polling")
    public ResponseEntity<Map<String, String>> createPollingOptimisticReservation(@RequestBody ReservationRequest request) {
        return enqueue(request, ReservationQueueLockType.OPTIMISTIC);
    }

    /**
     * [v4-pes] 비동기 대기열 + 비관적 락
     */
    @PostMapping("/v4-pes/queue-polling")
    public ResponseEntity<Map<String, String>> createPollingPessimisticReservation(@RequestBody ReservationRequest request) {
        return enqueue(request, ReservationQueueLockType.PESSIMISTIC);
    }

    /**
     * [v4/status] 비동기 예약 상태 조회 (Polling용)
     */
    @GetMapping("/v4/status")
    public ResponseEntity<Map<String, String>> getReservationStatus(
            @RequestParam Long userId, 
            @RequestParam Long seatId) {
        String status = reservationQueueOrchestrationUseCase.getStatus(userId, seatId);
        return ResponseEntity.ok(Map.of("status", status != null ? status : "NOT_FOUND"));
    }

    /**
     * [v5] SSE 실시간 구독
     */
    @GetMapping(value = "/v5/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam Long userId,
            @RequestParam Long seatId) {
        return realtimeSubscriptionUseCase.subscribeReservationSse(userId, seatId);
    }

    /**
     * [v5-opt] 비동기 대기열 + SSE + 낙관적 락
     */
    @PostMapping("/v5-opt/queue-sse")
    public ResponseEntity<Map<String, String>> createSseOptimisticReservation(@RequestBody ReservationRequest request) {
        return enqueue(request, ReservationQueueLockType.OPTIMISTIC);
    }

    /**
     * [v6] Step 9 - 좌석 홀드 생성
     */
    @PostMapping("/v6/holds")
    public ResponseEntity<ReservationLifecycleResponse> createHold(@RequestBody ReservationRequest request) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.createHold(toCreateCommand(request));
        return ResponseEntity.status(201).body(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v6] Step 9 - 결제 진행 상태 전이 (HOLD -> PAYING)
     */
    @PostMapping("/v6/{reservationId}/paying")
    public ResponseEntity<ReservationLifecycleResponse> startPaying(
            @PathVariable Long reservationId,
            @RequestBody ReservationStateRequest request) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.startPaying(reservationId, request.getUserId());
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v6] Step 9 - 결제 확정 상태 전이 (PAYING -> CONFIRMED)
     */
    @PostMapping("/v6/{reservationId}/confirm")
    public ResponseEntity<ReservationLifecycleResponse> confirm(
            @PathVariable Long reservationId,
            @RequestBody ReservationStateRequest request) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.confirm(
                reservationId,
                request.getUserId(),
                request.getPaymentMethod()
        );
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v6] Step 10 - 예약 취소 상태 전이 (CONFIRMED -> CANCELLED)
     */
    @PostMapping("/v6/{reservationId}/cancel")
    public ResponseEntity<ReservationLifecycleResponse> cancel(
            @PathVariable Long reservationId,
            @RequestBody ReservationStateRequest request) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.cancel(reservationId, request.getUserId());
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v6] Step 10 - 환불 완료 상태 전이 (CANCELLED -> REFUNDED)
     */
    @PostMapping("/v6/{reservationId}/refund")
    public ResponseEntity<ReservationLifecycleResponse> refund(
            @PathVariable Long reservationId,
            @RequestBody ReservationStateRequest request) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.refund(reservationId, request.getUserId());
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v6] Step 9 - 예약 상태 조회
     */
    @GetMapping("/v6/{reservationId}")
    public ResponseEntity<ReservationLifecycleResponse> getReservation(
            @PathVariable Long reservationId,
            @RequestParam Long userId) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.getReservation(reservationId, userId);
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v6] Step 12 - 부정사용/감사 추적 로그 조회
     */
    @GetMapping("/v6/audit/abuse")
    public ResponseEntity<List<AbuseAuditResponse>> getAbuseAudits(
            @RequestParam(required = false) AbuseAuditActionType action,
            @RequestParam(required = false) AbuseAuditResultType result,
            @RequestParam(required = false) AbuseAuditReasonType reason,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long concertId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toAt,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(
                abuseAuditUseCase.getAuditLogs(action, result, reason, userId, concertId, fromAt, toAt, limit)
                        .stream()
                        .map(AbuseAuditResponse::from)
                        .toList()
        );
    }

    /**
     * [v7] Auth Track A2 - 좌석 선택 soft lock 획득
     */
    @PostMapping("/v7/locks/seats/{seatId}")
    public ResponseEntity<SeatSoftLockAcquireResponse> acquireSeatSoftLockV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long seatId,
            @RequestBody(required = false) SeatSoftLockRequest request
    ) {
        String requestId = request == null ? null : request.getRequestId();
        return ResponseEntity.ok(
                SeatSoftLockAcquireResponse.from(
                        seatSoftLockUseCase.acquire(requiredUserId(principal), seatId, requestId)
                )
        );
    }

    /**
     * [v7] Auth Track A2 - 좌석 선택 soft lock 해제
     */
    @DeleteMapping("/v7/locks/seats/{seatId}")
    public ResponseEntity<SeatSoftLockReleaseResponse> releaseSeatSoftLockV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long seatId
    ) {
        return ResponseEntity.ok(
                SeatSoftLockReleaseResponse.from(
                        seatSoftLockUseCase.release(requiredUserId(principal), seatId)
                )
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
        Long userId = requiredUserId(principal);
        seatSoftLockUseCase.ensureHoldableByUser(userId, request.getSeatId());
        ReservationLifecycleResult result = reservationLifecycleUseCase.createHold(
                toCreateCommand(request.toReservationRequest(userId))
        );
        seatSoftLockUseCase.promoteToHold(userId, result.getSeatId(), result.getHoldExpiresAt());
        return ResponseEntity.status(201).body(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 PAYING 전이
     */
    @PostMapping("/v7/{reservationId}/paying")
    public ResponseEntity<ReservationLifecycleResponse> startPayingV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.startPaying(reservationId, requiredUserId(principal));
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 CONFIRMED 전이
     */
    @PostMapping("/v7/{reservationId}/confirm")
    public ResponseEntity<ReservationLifecycleResponse> confirmV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId,
            @RequestBody(required = false) ReservationStateRequest request
    ) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.confirm(
                reservationId,
                requiredUserId(principal),
                request == null ? null : request.getPaymentMethod()
        );
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 CANCELLED 전이
     */
    @PostMapping("/v7/{reservationId}/cancel")
    public ResponseEntity<ReservationLifecycleResponse> cancelV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.cancel(reservationId, requiredUserId(principal));
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 REFUNDED 전이
     */
    @PostMapping("/v7/{reservationId}/refund")
    public ResponseEntity<ReservationLifecycleResponse> refundV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.refund(reservationId, requiredUserId(principal));
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v7] Admin - 환불 마감 이후 강제 환불(override)
     */
    @PostMapping("/v7/admin/{reservationId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationLifecycleResponse> refundAsAdminV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.refundAsAdmin(reservationId, requiredUserId(principal));
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v7] Auth Track A2 - 인증 사용자 기반 예약 상태 조회
     */
    @GetMapping("/v7/{reservationId}")
    public ResponseEntity<ReservationLifecycleResponse> getReservationV7(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long reservationId
    ) {
        ReservationLifecycleResult result = reservationLifecycleUseCase.getReservation(reservationId, requiredUserId(principal));
        return ResponseEntity.ok(ReservationLifecycleResponse.from(result));
    }

    /**
     * [v7] Auth Track A2 - 본인 예약 목록 조회
     */
    @GetMapping("/v7/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservationsV7(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        List<ReservationResponse> responses = reservationUseCase.getReservationsByUserId(requiredUserId(principal))
                .stream()
                .map(ReservationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * [v7] Auth Track A2 - 부정사용 감사 로그 조회 (관리자 권한 전용)
     */
    @GetMapping("/v7/audit/abuse")
    public ResponseEntity<List<AbuseAuditResponse>> getAbuseAuditsV7(
            @RequestParam(required = false) AbuseAuditActionType action,
            @RequestParam(required = false) AbuseAuditResultType result,
            @RequestParam(required = false) AbuseAuditReasonType reason,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long concertId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toAt,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(
                abuseAuditUseCase.getAuditLogs(action, result, reason, userId, concertId, fromAt, toAt, limit)
                        .stream()
                        .map(AbuseAuditResponse::from)
                        .toList()
        );
    }

    /**
     * [v7] Admin - 강제 환불 감사 로그 조회
     */
    @GetMapping("/v7/audit/admin-refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminRefundAuditResponse>> getAdminRefundAuditsV7(
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) AdminRefundAuditResultType result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toAt,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(
                adminRefundAuditUseCase.getAuditLogs(reservationId, actorUserId, result, fromAt, toAt, limit)
                        .stream()
                        .map(AdminRefundAuditResponse::from)
                        .toList()
        );
    }

    private ResponseEntity<Map<String, String>> enqueue(ReservationRequest request, ReservationQueueLockType lockType) {
        reservationQueueOrchestrationUseCase.enqueue(request.getUserId(), request.getSeatId(), lockType);
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
        List<ReservationResponse> responses = reservationUseCase.getReservationsByUserId(userId)
                .stream()
                .map(ReservationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * [Delete] 예약 취소
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationUseCase.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    private ReservationCreateCommand toCreateCommand(ReservationRequest request) {
        return new ReservationCreateCommand(
                request.getUserId(),
                request.getSeatId(),
                request.getRequestFingerprint(),
                request.getDeviceFingerprint()
        );
    }
}
