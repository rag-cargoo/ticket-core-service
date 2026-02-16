package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.reservation.ReservationLifecycleResponse;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueSsePayload;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueStatus;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.port.outbound.ReservationSeatPort;
import com.ticketrush.domain.reservation.port.outbound.ReservationUserPort;
import com.ticketrush.domain.reservation.port.outbound.ReservationWaitingQueuePort;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.global.config.ReservationProperties;
import com.ticketrush.global.cache.ConcertReadCacheEvictor;
import com.ticketrush.global.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationLifecycleServiceImpl implements ReservationLifecycleService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatPort reservationSeatPort;
    private final ReservationUserPort reservationUserPort;
    private final ReservationWaitingQueuePort reservationWaitingQueuePort;
    private final ReservationProperties reservationProperties;
    private final SalesPolicyService salesPolicyService;
    private final AbuseAuditService abuseAuditService;
    private final SseEmitterManager sseEmitterManager;
    private final ConcertReadCacheEvictor concertReadCacheEvictor;

    @Transactional
    public ReservationLifecycleResponse createHold(ReservationRequest request) {
        User user = reservationUserPort.getUser(request.getUserId());
        Seat seat = reservationSeatPort.getSeatWithPessimisticLock(request.getSeatId());

        LocalDateTime now = LocalDateTime.now();
        salesPolicyService.validateHoldRequest(user, seat, now);
        abuseAuditService.validateHoldRequest(request, user, seat, now);
        seat.hold();
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(seat.getConcertOption().getId());

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
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(reservation.getSeat().getConcertOption().getId());
        return ReservationLifecycleResponse.from(reservation);
    }

    @Transactional
    public ReservationLifecycleResponse cancel(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        LocalDateTime now = LocalDateTime.now();
        reservation.cancel(now);
        reservation.getSeat().cancel();
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(reservation.getSeat().getConcertOption().getId());

        Long concertId = reservation.getSeat().getConcertOption().getConcert().getId();
        List<Long> activatedUsers = reservationWaitingQueuePort.activateUsers(concertId, 1);
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

        Set<Long> changedOptionIds = new HashSet<>();
        for (Reservation reservation : expiredTargets) {
            reservation.expire(now);
            reservation.getSeat().cancel();
            changedOptionIds.add(reservation.getSeat().getConcertOption().getId());
        }
        for (Long optionId : changedOptionIds) {
            concertReadCacheEvictor.evictAvailableSeatsByOptionId(optionId);
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
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(reservation.getSeat().getConcertOption().getId());
    }

    private void notifyActivatedUsers(Long concertId, List<Long> activatedUsers) {
        for (Long activatedUserId : activatedUsers) {
            Long activeTtlSeconds = reservationWaitingQueuePort.getActiveTtlSeconds(activatedUserId);
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
