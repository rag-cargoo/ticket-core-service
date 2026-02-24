package com.ticketrush.application.reservation.service;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationLifecycleResult;
import com.ticketrush.application.payment.port.inbound.PaymentMethodCatalogUseCase;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.payment.entity.PaymentMethod;
import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.port.outbound.ReservationPaymentPort;
import com.ticketrush.domain.reservation.port.outbound.ReservationSeatPort;
import com.ticketrush.domain.reservation.port.outbound.ReservationUserPort;
import com.ticketrush.domain.reservation.port.outbound.ReservationWaitingQueuePort;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.application.reservation.service.AbuseAuditService;
import com.ticketrush.application.reservation.service.AdminRefundAuditService;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.application.reservation.port.outbound.PaymentConfigPort;
import com.ticketrush.application.reservation.port.outbound.ReservationConfigPort;
import com.ticketrush.application.concert.port.outbound.ConcertReadCacheEvictPort;
import com.ticketrush.application.port.outbound.QueuePushPayload;
import com.ticketrush.application.port.outbound.QueueRuntimePushPort;
import com.ticketrush.application.port.outbound.ReservationStatusPushPort;
import com.ticketrush.application.port.outbound.SeatMapPushPort;
import com.ticketrush.application.waitingqueue.model.WaitingQueueStatusType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ReservationLifecycleServiceImpl implements ReservationLifecycleService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatPort reservationSeatPort;
    private final ReservationUserPort reservationUserPort;
    private final ReservationWaitingQueuePort reservationWaitingQueuePort;
    private final ReservationConfigPort reservationProperties;
    private final PaymentConfigPort paymentProperties;
    private final PaymentMethodCatalogUseCase paymentMethodCatalogUseCase;
    private final SalesPolicyService salesPolicyService;
    private final AbuseAuditService abuseAuditService;
    private final AdminRefundAuditService adminRefundAuditService;
    private final ReservationPaymentPort reservationPaymentPort;
    private final QueueRuntimePushPort queuePushNotifier;
    private final ReservationStatusPushPort reservationStatusPushNotifier;
    private final SeatMapPushPort seatMapPushNotifier;
    private final ConcertReadCacheEvictPort concertReadCacheEvictor;

    public ReservationLifecycleServiceImpl(
            ReservationRepository reservationRepository,
            ReservationSeatPort reservationSeatPort,
            ReservationUserPort reservationUserPort,
            ReservationWaitingQueuePort reservationWaitingQueuePort,
            ReservationConfigPort reservationProperties,
            PaymentConfigPort paymentProperties,
            PaymentMethodCatalogUseCase paymentMethodCatalogUseCase,
            SalesPolicyService salesPolicyService,
            AbuseAuditService abuseAuditService,
            AdminRefundAuditService adminRefundAuditService,
            ReservationPaymentPort reservationPaymentPort,
            @Qualifier("queueRuntimePushNotifier") QueueRuntimePushPort queuePushNotifier,
            @Qualifier("reservationStatusPushNotifier") ReservationStatusPushPort reservationStatusPushNotifier,
            @Qualifier("seatMapPushNotifier") SeatMapPushPort seatMapPushNotifier,
            ConcertReadCacheEvictPort concertReadCacheEvictor
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationSeatPort = reservationSeatPort;
        this.reservationUserPort = reservationUserPort;
        this.reservationWaitingQueuePort = reservationWaitingQueuePort;
        this.reservationProperties = reservationProperties;
        this.paymentProperties = paymentProperties;
        this.paymentMethodCatalogUseCase = paymentMethodCatalogUseCase;
        this.salesPolicyService = salesPolicyService;
        this.abuseAuditService = abuseAuditService;
        this.adminRefundAuditService = adminRefundAuditService;
        this.reservationPaymentPort = reservationPaymentPort;
        this.queuePushNotifier = queuePushNotifier;
        this.reservationStatusPushNotifier = reservationStatusPushNotifier;
        this.seatMapPushNotifier = seatMapPushNotifier;
        this.concertReadCacheEvictor = concertReadCacheEvictor;
    }

    @Transactional
    public ReservationLifecycleResult createHold(ReservationCreateCommand command) {
        User user = reservationUserPort.getUser(command.getUserId());
        Seat seat = reservationSeatPort.getSeatWithPessimisticLock(command.getSeatId());

        LocalDateTime now = LocalDateTime.now();
        salesPolicyService.validateHoldRequest(user, seat, now);
        abuseAuditService.validateHoldRequest(command.getRequestFingerprint(), command.getDeviceFingerprint(), user, seat, now);
        seat.hold();
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(seat.getConcertOption().getId());

        LocalDateTime holdExpiresAt = now.plusSeconds(reservationProperties.getHoldTtlSeconds());
        Reservation reservation = Reservation.hold(user, seat, now, holdExpiresAt);
        reservationRepository.save(reservation);
        abuseAuditService.recordAllowedHold(
                command.getRequestFingerprint(),
                command.getDeviceFingerprint(),
                user,
                seat,
                reservation.getId(),
                now
        );
        seatMapPushNotifier.sendSeatMapStatus(
                seat.getConcertOption().getId(),
                seat.getId(),
                Reservation.ReservationStatus.HOLD.name(),
                user.getId(),
                holdExpiresAt.toString()
        );
        return ReservationLifecycleResult.from(reservation);
    }

    @Transactional
    public ReservationLifecycleResult startPaying(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        LocalDateTime now = LocalDateTime.now();
        expireIfNeeded(reservation, now);
        reservation.startPaying(now);
        return ReservationLifecycleResult.from(reservation);
    }

    @Transactional
    public ReservationLifecycleResult confirm(Long reservationId, Long userId) {
        return confirm(reservationId, userId, null);
    }

    @Transactional
    public ReservationLifecycleResult confirm(Long reservationId, Long userId, String paymentMethodValue) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        LocalDateTime now = LocalDateTime.now();
        expireIfNeeded(reservation, now);
        if (reservation.getStatus() == Reservation.ReservationStatus.CONFIRMED) {
            return ReservationLifecycleResult.from(reservation);
        }
        if (reservation.getStatus() != Reservation.ReservationStatus.PAYING) {
            throw new IllegalStateException(
                    "Only PAYING reservation can transition to CONFIRMED. currentStatus=" + reservation.getStatus()
            );
        }

        PaymentMethod paymentMethod = resolvePaymentMethod(paymentMethodValue);
        paymentMethodCatalogUseCase.assertMethodAvailable(paymentMethod.name());
        Long paymentAmount = resolvePaymentAmount(reservation);
        PaymentTransaction paymentTransaction = reservationPaymentPort.payForReservation(
                userId,
                reservationId,
                paymentAmount,
                paymentMethod,
                "reservation-payment-" + reservationId
        );

        if (paymentTransaction.isStatus(PaymentTransactionStatus.SUCCESS)) {
            confirmReservationAndSeat(reservation, now);
        }

        String paymentMethodCode = paymentTransaction.getPaymentMethod() == null
                ? paymentMethod.name()
                : paymentTransaction.getPaymentMethod().name();
        String paymentProvider = resolvePaymentProvider(paymentTransaction);
        String paymentStatus = paymentTransaction.getStatus() == null ? null : paymentTransaction.getStatus().name();
        String paymentAction = resolvePaymentAction(paymentTransaction.getStatus(), paymentProvider);
        String paymentRedirectUrl = resolvePaymentRedirectUrl(
                paymentAction,
                reservationId,
                userId,
                paymentAmount,
                paymentMethodCode,
                paymentTransaction
        );

        return ReservationLifecycleResult.from(
                reservation,
                paymentMethodCode,
                paymentProvider,
                paymentStatus,
                paymentTransaction.getId(),
                paymentAction,
                paymentRedirectUrl
        );
    }

    @Transactional
    public ReservationLifecycleResult cancel(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        LocalDateTime now = LocalDateTime.now();
        reservation.cancel(now);
        reservation.getSeat().cancel();
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(reservation.getSeat().getConcertOption().getId());
        seatMapPushNotifier.sendSeatMapStatus(
                reservation.getSeat().getConcertOption().getId(),
                reservation.getSeat().getId(),
                Seat.SeatStatus.AVAILABLE.name(),
                null,
                null
        );

        Long concertId = reservation.getSeat().getConcertOption().getConcert().getId();
        List<Long> activatedUsers = reservationWaitingQueuePort.activateUsers(concertId, 1);
        notifyActivatedUsers(concertId, activatedUsers);

        return ReservationLifecycleResult.from(reservation, activatedUsers);
    }

    @Transactional
    public ReservationLifecycleResult refund(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        return refundInternal(reservation, userId, userId, false);
    }

    @Transactional
    public ReservationLifecycleResult refundAsAdmin(Long reservationId, Long adminUserId) {
        User adminUser = reservationUserPort.getUser(adminUserId);
        if (adminUser.getRole() != UserRole.ADMIN) {
            adminRefundAuditService.recordDenied(
                    reservationId,
                    null,
                    adminUserId,
                    adminUser.getRole().name(),
                    "Admin override refund requires ADMIN role"
            );
            throw new IllegalStateException("Admin override refund requires ADMIN role. userId=" + adminUserId);
        }
        Reservation reservation = getReservation(reservationId);
        Long reservationOwnerId = reservation.getUser().getId();
        try {
            ReservationLifecycleResult response = refundInternal(reservation, reservationOwnerId, adminUserId, true);
            adminRefundAuditService.recordSuccess(
                    reservationId,
                    reservationOwnerId,
                    adminUserId,
                    adminUser.getRole().name(),
                    "Admin override refund completed"
            );
            return response;
        } catch (RuntimeException exception) {
            adminRefundAuditService.recordFailed(
                    reservationId,
                    reservationOwnerId,
                    adminUserId,
                    adminUser.getRole().name(),
                    exception.getMessage()
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ReservationLifecycleResult getReservation(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        return ReservationLifecycleResult.from(reservation);
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
            seatMapPushNotifier.sendSeatMapStatus(
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
            QueuePushPayload payload = QueuePushPayload.of(
                    activatedUserId,
                    concertId,
                    WaitingQueueStatusType.ACTIVE.name(),
                    0L,
                    activeTtlSeconds
            );
            queuePushNotifier.sendQueueActivated(activatedUserId, concertId, payload);
        }
    }

    private ReservationLifecycleResult refundInternal(
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
        return ReservationLifecycleResult.from(reservation);
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
        reservationStatusPushNotifier.sendReservationStatus(
                reservation.getUser().getId(),
                reservation.getSeat().getId(),
                Reservation.ReservationStatus.EXPIRED.name()
        );
    }

    private void confirmReservationAndSeat(Reservation reservation, LocalDateTime now) {
        reservation.confirmPayment(now);
        reservation.getSeat().confirmHeldSeat();
        concertReadCacheEvictor.evictAvailableSeatsByOptionId(reservation.getSeat().getConcertOption().getId());
        seatMapPushNotifier.sendSeatMapStatus(
                reservation.getSeat().getConcertOption().getId(),
                reservation.getSeat().getId(),
                Reservation.ReservationStatus.CONFIRMED.name(),
                reservation.getUser().getId(),
                null
        );
        reservationStatusPushNotifier.sendReservationStatus(
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

    private PaymentMethod resolvePaymentMethod(String value) {
        try {
            return PaymentMethod.fromNullable(value, PaymentMethod.WALLET);
        } catch (IllegalArgumentException exception) {
            String safeValue = value == null ? "" : value.trim();
            throw new IllegalStateException(
                    "Unsupported payment method: " + safeValue
                            + ". allowed=WALLET,CARD,KAKAOPAY,NAVERPAY,BANK_TRANSFER",
                    exception
            );
        }
    }

    private String resolvePaymentProvider(PaymentTransaction paymentTransaction) {
        if (StringUtils.hasText(paymentTransaction.getPaymentProvider())) {
            return paymentTransaction.getPaymentProvider().trim();
        }
        return paymentProperties.getProvider();
    }

    private String resolvePaymentAction(PaymentTransactionStatus paymentStatus, String paymentProvider) {
        if (paymentStatus == null || paymentStatus == PaymentTransactionStatus.SUCCESS) {
            return "NONE";
        }
        if (paymentStatus == PaymentTransactionStatus.PENDING) {
            if ("pg-ready".equalsIgnoreCase(paymentProvider) && paymentProperties.isExternalLiveEnabled()) {
                return "REDIRECT";
            }
            return "WAIT_WEBHOOK";
        }
        return "RETRY_CONFIRM";
    }

    private String resolvePaymentRedirectUrl(
            String paymentAction,
            Long reservationId,
            Long userId,
            Long paymentAmount,
            String paymentMethod,
            PaymentTransaction paymentTransaction
    ) {
        if (!"REDIRECT".equals(paymentAction)) {
            return null;
        }
        String checkoutBaseUrl = paymentProperties.getPgReadyCheckoutBaseUrl();
        if (!StringUtils.hasText(checkoutBaseUrl)) {
            return null;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(checkoutBaseUrl.trim())
                .queryParam("reservationId", reservationId)
                .queryParam("paymentTransactionId", paymentTransaction.getId())
                .queryParam("userId", userId)
                .queryParam("amount", paymentAmount)
                .queryParam("paymentMethod", paymentMethod);
        appendIfText(builder, "orderId", paymentTransaction.getIdempotencyKey());
        appendIfText(builder, "callbackUrl", paymentProperties.getPgReadyCallbackUrl());
        appendIfText(builder, "successRedirectUrl", paymentProperties.getPgReadySuccessRedirectUrl());
        appendIfText(builder, "cancelRedirectUrl", paymentProperties.getPgReadyCancelRedirectUrl());
        appendIfText(builder, "failureRedirectUrl", paymentProperties.getPgReadyFailureRedirectUrl());
        return builder.build().encode().toUriString();
    }

    private void appendIfText(UriComponentsBuilder builder, String name, String value) {
        if (StringUtils.hasText(value)) {
            builder.queryParam(name, value.trim());
        }
    }
}
