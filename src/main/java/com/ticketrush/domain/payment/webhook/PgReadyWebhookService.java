package com.ticketrush.domain.payment.webhook;

import com.ticketrush.api.dto.payment.PgReadyWebhookRequest;
import com.ticketrush.api.dto.payment.PgReadyWebhookResponse;
import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.payment.entity.PaymentTransactionType;
import com.ticketrush.domain.payment.repository.PaymentTransactionRepository;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.global.cache.ConcertReadCacheEvictor;
import com.ticketrush.global.push.PushNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PgReadyWebhookService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReservationRepository reservationRepository;
    private final PushNotifier pushNotifier;
    private final ConcertReadCacheEvictor concertReadCacheEvictor;

    private static final String EVENT_PAYMENT = "PAYMENT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_FAILED = "FAILED";

    @Transactional
    public PgReadyWebhookResponse handle(PgReadyWebhookRequest request) {
        String eventType = normalizeUpper(request.getEventType());
        String status = normalizeUpper(request.getStatus());
        Long reservationId = request.getReservationId();
        String providerEventId = normalizeRaw(request.getProviderEventId());

        if (!StringUtils.hasText(eventType)) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("status is required");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId is required");
        }

        if (!EVENT_PAYMENT.equals(eventType)) {
            return acceptedResponse(eventType, status, reservationId, "ignored unsupported eventType");
        }

        PaymentTransaction paymentTransaction = resolvePaymentTransaction(request, reservationId);
        if (STATUS_APPROVED.equals(status)) {
            handleApproved(paymentTransaction, reservationId, providerEventId);
            return acceptedResponse(eventType, status, reservationId, "applied approved");
        }
        if (STATUS_FAILED.equals(status)) {
            handleFailed(paymentTransaction, providerEventId);
            return acceptedResponse(eventType, status, reservationId, "applied failed");
        }

        log.info(
                ">>>> [PgReadyWebhook] ignored unsupported status eventType={}, status={}, reservationId={}, providerEventId={}",
                eventType,
                status,
                reservationId,
                providerEventId
        );

        return acceptedResponse(eventType, status, reservationId, "ignored unsupported status");
    }

    private PaymentTransaction resolvePaymentTransaction(PgReadyWebhookRequest request, Long reservationId) {
        String idempotencyKey = normalizeRaw(request.getIdempotencyKey());
        if (StringUtils.hasText(idempotencyKey)) {
            return paymentTransactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Payment transaction not found. idempotencyKey=" + idempotencyKey));
        }
        return paymentTransactionRepository
                .findTopByReservationIdAndTypeOrderByIdDesc(reservationId, PaymentTransactionType.PAYMENT)
                .orElseThrow(() -> new IllegalStateException("Payment transaction not found for reservation: " + reservationId));
    }

    private void handleApproved(PaymentTransaction paymentTransaction, Long reservationId, String providerEventId) {
        if (paymentTransaction.isStatus(PaymentTransactionStatus.SUCCESS)) {
            log.info(
                    ">>>> [PgReadyWebhook] already approved reservationId={}, txId={}",
                    reservationId,
                    paymentTransaction.getId()
            );
            return;
        }

        paymentTransaction.markSuccess(buildDescription("PG_READY_PAYMENT_APPROVED", providerEventId));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalStateException("Reservation not found. reservationId=" + reservationId));

        if (reservation.getStatus() == Reservation.ReservationStatus.PAYING) {
            LocalDateTime now = LocalDateTime.now();
            reservation.confirmPayment(now);
            reservation.getSeat().confirmHeldSeat();
            concertReadCacheEvictor.evictAvailableSeatsByOptionId(reservation.getSeat().getConcertOption().getId());
            pushNotifier.sendReservationStatus(
                    reservation.getUser().getId(),
                    reservation.getSeat().getId(),
                    Reservation.ReservationStatus.CONFIRMED.name()
            );
            return;
        }

        log.warn(
                ">>>> [PgReadyWebhook] approved but reservation not PAYING. reservationId={}, reservationStatus={}, txId={}",
                reservationId,
                reservation.getStatus(),
                paymentTransaction.getId()
        );
    }

    private void handleFailed(PaymentTransaction paymentTransaction, String providerEventId) {
        if (paymentTransaction.isStatus(PaymentTransactionStatus.FAILED)) {
            return;
        }
        paymentTransaction.markFailed(buildDescription("PG_READY_PAYMENT_FAILED", providerEventId));
    }

    private PgReadyWebhookResponse acceptedResponse(String eventType, String status, Long reservationId, String message) {
        return new PgReadyWebhookResponse("pg-ready", eventType, status, reservationId, true, message);
    }

    private String buildDescription(String base, String providerEventId) {
        if (!StringUtils.hasText(providerEventId)) {
            return base;
        }
        return base + ":" + providerEventId;
    }

    private String normalizeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private String normalizeRaw(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }
}
