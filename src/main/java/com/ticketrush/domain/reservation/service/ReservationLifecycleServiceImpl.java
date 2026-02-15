package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.reservation.ReservationLifecycleResponse;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueSsePayload;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueStatus;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.global.config.ReservationProperties;
import com.ticketrush.domain.waitingqueue.service.WaitingQueueService;
import com.ticketrush.global.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationLifecycleServiceImpl implements ReservationLifecycleService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ReservationProperties reservationProperties;
    private final SalesPolicyService salesPolicyService;
    private final AbuseAuditService abuseAuditService;
    private final WaitingQueueService waitingQueueService;
    private final SseEmitterManager sseEmitterManager;

    @Transactional
    public ReservationLifecycleResponse createHold(ReservationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));
        Seat seat = seatRepository.findByIdWithPessimisticLock(request.getSeatId())
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + request.getSeatId()));

        LocalDateTime now = LocalDateTime.now();
        salesPolicyService.validateHoldRequest(user, seat, now);
        abuseAuditService.validateHoldRequest(request, user, seat, now);
        seat.hold();

        LocalDateTime holdExpiresAt = now.plusSeconds(reservationProperties.getHoldTtlSeconds());
        Reservation reservation = Reservation.hold(user, seat, now, holdExpiresAt);
        reservationRepository.save(reservation);
        abuseAuditService.recordAllowedHold(request, user, seat, reservation.getId(), now);
        return ReservationLifecycleResponse.from(reservation);
    }

    @Transactional
    public ReservationLifecycleResponse startPaying(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        LocalDateTime now = LocalDateTime.now();
        expireIfNeeded(reservation, now);
        reservation.startPaying(now);
        return ReservationLifecycleResponse.from(reservation);
    }

    @Transactional
    public ReservationLifecycleResponse confirm(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        LocalDateTime now = LocalDateTime.now();
        expireIfNeeded(reservation, now);
        reservation.confirmPayment(now);
        reservation.getSeat().confirmHeldSeat();
        return ReservationLifecycleResponse.from(reservation);
    }

    @Transactional
    public ReservationLifecycleResponse cancel(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        LocalDateTime now = LocalDateTime.now();
        reservation.cancel(now);
        reservation.getSeat().cancel();

        Long concertId = reservation.getSeat().getConcertOption().getConcert().getId();
        List<Long> activatedUsers = waitingQueueService.activateUsers(concertId, 1);
        notifyActivatedUsers(concertId, activatedUsers);

        return ReservationLifecycleResponse.from(reservation, activatedUsers);
    }

    @Transactional
    public ReservationLifecycleResponse refund(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        reservation.refund(LocalDateTime.now());
        return ReservationLifecycleResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationLifecycleResponse getReservation(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        return ReservationLifecycleResponse.from(reservation);
    }

    @Transactional
    public int expireTimedOutHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredTargets = reservationRepository.findByStatusInAndHoldExpiresAtBefore(
                List.of(Reservation.ReservationStatus.HOLD, Reservation.ReservationStatus.PAYING),
                now
        );

        for (Reservation reservation : expiredTargets) {
            reservation.expire(now);
            reservation.getSeat().cancel();
        }

        if (!expiredTargets.isEmpty()) {
            log.info(">>>> [ReservationLifecycle] expired holds: {}", expiredTargets.size());
        }
        return expiredTargets.size();
    }

    private Reservation getOwnedReservation(Long reservationId, Long userId) {
        return reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found or not owned by user. reservationId=" + reservationId + ", userId=" + userId
                ));
    }

    private void expireIfNeeded(Reservation reservation, LocalDateTime now) {
        if (!reservation.isHoldInProgress() || !reservation.isExpired(now)) {
            return;
        }
        reservation.expire(now);
        reservation.getSeat().cancel();
    }

    private void notifyActivatedUsers(Long concertId, List<Long> activatedUsers) {
        for (Long activatedUserId : activatedUsers) {
            Long activeTtlSeconds = waitingQueueService.getActiveTtlSeconds(activatedUserId);
            WaitingQueueSsePayload payload = WaitingQueueSsePayload.builder()
                    .userId(activatedUserId)
                    .concertId(concertId)
                    .status(WaitingQueueStatus.ACTIVE.name())
                    .rank(0L)
                    .activeTtlSeconds(activeTtlSeconds)
                    .timestamp(Instant.now().toString())
                    .build();
            sseEmitterManager.sendQueueActivated(activatedUserId, concertId, payload);
        }
    }
}
