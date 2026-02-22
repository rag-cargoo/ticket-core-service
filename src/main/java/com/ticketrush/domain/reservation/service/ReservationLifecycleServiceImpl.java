package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.reservation.ReservationLifecycleResponse;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueSsePayload;
import com.ticketrush.api.dto.waitingqueue.WaitingQueueStatus;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.port.outbound.ReservationPaymentPort;
import com.ticketrush.domain.reservation.port.outbound.ReservationSeatPort;
import com.ticketrush.domain.reservation.port.outbound.ReservationUserPort;
import com.ticketrush.domain.reservation.port.outbound.ReservationWaitingQueuePort;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.global.config.ReservationProperties;
import com.ticketrush.global.config.PaymentProperties;
import com.ticketrush.global.cache.ConcertReadCacheEvictor;
import com.ticketrush.global.push.PushNotifier;
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
    private final PaymentProperties paymentProperties;
    private final SalesPolicyService salesPolicyService;
    private final AbuseAuditService abuseAuditService;
    private final AdminRefundAuditService adminRefundAuditService;
    private final ReservationPaymentPort reservationPaymentPort;
    private final PushNotifier pushNotifier;
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
        pushNotifier.sendSeatMapStatus(
                seat.getConcertOption().getId(),
                seat.getId(),
                Reservation.ReservationStatus.HOLD.name(),
                user.getId(),
                holdExpiresAt.toString()
        );
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
        if (reservation.getStatus() == Reservation.ReservationStatus.CONFIRMED) {
            return ReservationLifecycleResponse.from(reservation);
        }
        if (reservation.getStatus() != Reservation.ReservationStatus.PAYING) {
            throw new IllegalStateException(
                    "Only PAYING reservation can transition to CONFIRMED. currentStatus=" + reservation.getStatus()
            );
        }

        Long paymentAmount = resolvePaymentAmount(reservation);
        PaymentTransaction paymentTransaction = reservationPaymentPort.payForReservation(
                userId,
                reservationId,
                paymentAmount,
                "reservation-payment-" + reservationId
        );

        if (paymentTransaction.isStatus(PaymentTransactionStatus.SUCCESS)) {
            confirmReservationAndSeat(reservation, now);
        }

        return ReservationLifecycleResponse.from(reservation);
    }

    @Transactional
    public ReservationLifecycleResponse cancel(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        LocalDateTime now = LocalDateTime.now();
        reservation.cancel(now);
        reservation.getSeat().cancel();
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(reservation.getSeat().getConcertOption().getId());
        pushNotifier.sendSeatMapStatus(
                reservation.getSeat().getConcertOption().getId(),
                reservation.getSeat().getId(),
                Seat.SeatStatus.AVAILABLE.name(),
                null,
                null
        );

        Long concertId = reservation.getSeat().getConcertOption().getConcert().getId();
        List<Long> activatedUsers = reservationWaitingQueuePort.activateUsers(concertId, 1);
        notifyActivatedUsers(concertId, activatedUsers);

        return ReservationLifecycleResponse.from(reservation, activatedUsers);
    }

    @Transactional
    public ReservationLifecycleResponse refund(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        return refundInternal(reservation, userId, userId, false);
    }

    @Transactional
    public ReservationLifecycleResponse refundAsAdmin(Long reservationId, Long adminUserId) {
        User adminUser = reservationUserPort.getUser(adminUserId);
        if (adminUser.getRole() != UserRole.ADMIN) {
            adminRefundAuditService.recordDenied(
                    reservationId,
                    null,
                    adminUserId,
                    adminUser.getRole(),
                    "Admin override refund requires ADMIN role"
            );
            throw new IllegalStateException("Admin override refund requires ADMIN role. userId=" + adminUserId);
        }
        Reservation reservation = getReservation(reservationId);
        Long reservationOwnerId = reservation.getUser().getId();
        try {
            ReservationLifecycleResponse response = refundInternal(reservation, reservationOwnerId, adminUserId, true);
            adminRefundAuditService.recordSuccess(
                    reservationId,
                    reservationOwnerId,
                    adminUserId,
                    adminUser.getRole(),
                    "Admin override refund completed"
            );
            return response;
        } catch (RuntimeException exception) {
            adminRefundAuditService.recordFailed(
                    reservationId,
                    reservationOwnerId,
                    adminUserId,
                    adminUser.getRole(),
                    exception.getMessage()
            );
            throw exception;
        }
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
            notifyReservationExpired(reservation);
            pushNotifier.sendSeatMapStatus(
                    reservation.getSeat().getConcertOption().getId(),
                    reservation.getSeat().getId(),
                    Seat.SeatStatus.AVAILABLE.name(),
                    null,
                    null
            );
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

    private Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found. reservationId=" + reservationId));
    }

    private void expireIfNeeded(Reservation reservation, LocalDateTime now) {
        if (!reservation.isHoldInProgress() || !reservation.isExpired(now)) {
            return;
        }
        reservation.expire(now);
        reservation.getSeat().cancel();
        notifyReservationExpired(reservation);
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
            pushNotifier.sendQueueActivated(activatedUserId, concertId, payload);
        }
    }

    private ReservationLifecycleResponse refundInternal(
            Reservation reservation,
            Long paymentUserId,
            Long actorUserId,
            boolean allowAdminOverride
    ) {
        LocalDateTime now = LocalDateTime.now();
        validateRefundCutoff(reservation, now, allowAdminOverride, actorUserId);
        Long reservationId = reservation.getId();
        PaymentTransaction refundTransaction = reservationPaymentPort.refundReservation(
                paymentUserId,
                reservationId,
                "reservation-refund-" + reservationId
        );
        if (!refundTransaction.isStatus(PaymentTransactionStatus.SUCCESS)) {
            throw new IllegalStateException(
                    "Refund not completed yet. reservationId=" + reservationId + ", status=" + refundTransaction.getStatus()
            );
        }
        reservation.refund(now);
        return ReservationLifecycleResponse.from(reservation);
    }

    private void validateRefundCutoff(
            Reservation reservation,
            LocalDateTime now,
            boolean allowAdminOverride,
            Long actorUserId
    ) {
        long cutoffHours = Math.max(0, reservationProperties.getRefundCutoffHoursBeforeConcert());
        LocalDateTime concertDate = reservation.getSeat().getConcertOption().getConcertDate();
        LocalDateTime cutoffAt = concertDate.minusHours(cutoffHours);
        boolean isPastCutoff = !now.isBefore(cutoffAt);
        if (!isPastCutoff) {
            return;
        }
        if (allowAdminOverride) {
            log.info(
                    ">>>> [ReservationLifecycle] admin override refund cutoff: reservationId={}, adminUserId={}, concertDate={}, cutoffAt={}",
                    reservation.getId(),
                    actorUserId,
                    concertDate,
                    cutoffAt
            );
            return;
        }
        throw new IllegalStateException(
                "Refund cutoff passed. reservationId=" + reservation.getId() + ", cutoffAt=" + cutoffAt + ", concertDate=" + concertDate
        );
    }

    private void notifyReservationExpired(Reservation reservation) {
        pushNotifier.sendReservationStatus(
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                Reservation.ReservationStatus.EXPIRED.name()
        );
    }

    private void confirmReservationAndSeat(Reservation reservation, LocalDateTime now) {
        reservation.confirmPayment(now);
        reservation.getSeat().confirmHeldSeat();
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(reservation.getSeat().getConcertOption().getId());
        pushNotifier.sendSeatMapStatus(
                reservation.getSeat().getConcertOption().getId(),
                reservation.getSeat().getId(),
                Reservation.ReservationStatus.CONFIRMED.name(),
                reservation.getUser().getId(),
                null
        );
        pushNotifier.sendReservationStatus(
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                Reservation.ReservationStatus.CONFIRMED.name()
        );
    }

    private Long resolvePaymentAmount(Reservation reservation) {
        Long optionPriceAmount = reservation.getSeat().getConcertOption().getTicketPriceAmount();
        if (optionPriceAmount != null && optionPriceAmount >= 0L) {
            return optionPriceAmount;
        }
        return paymentProperties.getDefaultTicketPriceAmount();
    }
}
