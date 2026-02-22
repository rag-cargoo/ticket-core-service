package com.ticketrush.domain.reservation.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.api.dto.reservation.ReservationLifecycleResponse;
import com.ticketrush.domain.agency.Agency;
import com.ticketrush.domain.agency.AgencyRepository;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.adapter.outbound.ReservationSeatPortAdapter;
import com.ticketrush.domain.reservation.adapter.outbound.ReservationPaymentPortAdapter;
import com.ticketrush.domain.reservation.adapter.outbound.ReservationUserPortAdapter;
import com.ticketrush.domain.reservation.adapter.outbound.ReservationWaitingQueuePortAdapter;
import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.payment.service.PaymentService;
import com.ticketrush.domain.payment.service.PaymentServiceImpl;
import com.ticketrush.domain.payment.gateway.WalletPaymentGateway;
import com.ticketrush.domain.reservation.entity.AdminRefundAuditLog;
import com.ticketrush.domain.reservation.entity.AbuseAuditLog;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.entity.SalesPolicy;
import com.ticketrush.domain.reservation.repository.AdminRefundAuditLogRepository;
import com.ticketrush.domain.reservation.repository.AbuseAuditLogRepository;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.domain.user.UserTier;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.waitingqueue.service.WaitingQueueService;
import com.ticketrush.global.cache.ConcertReadCacheEvictor;
import com.ticketrush.global.config.AbuseGuardProperties;
import com.ticketrush.global.config.PaymentProperties;
import com.ticketrush.global.config.ReservationProperties;
import com.ticketrush.global.push.PushNotifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({
        ReservationLifecycleServiceImpl.class,
        SalesPolicyServiceImpl.class,
        AbuseAuditServiceImpl.class,
        AdminRefundAuditService.class,
        AbuseAuditWriter.class,
        PaymentServiceImpl.class,
        WalletPaymentGateway.class,
        ReservationSeatPortAdapter.class,
        ReservationPaymentPortAdapter.class,
        ReservationUserPortAdapter.class,
        ReservationWaitingQueuePortAdapter.class,
        ReservationLifecycleServiceIntegrationTest.TestConfig.class
})
class ReservationLifecycleServiceIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ReservationProperties reservationProperties() {
            ReservationProperties properties = new ReservationProperties();
            properties.setHoldTtlSeconds(1);
            properties.setExpireCheckDelayMillis(1000);
            properties.setRefundCutoffHoursBeforeConcert(24);
            return properties;
        }

        @Bean
        PaymentProperties paymentProperties() {
            PaymentProperties properties = new PaymentProperties();
            properties.setDefaultTicketPriceAmount(100_000L);
            return properties;
        }

        @Bean
        AbuseGuardProperties abuseGuardProperties() {
            AbuseGuardProperties properties = new AbuseGuardProperties();
            properties.setHoldRequestWindowSeconds(60);
            properties.setHoldRequestMaxCount(2);
            properties.setDuplicateRequestWindowSeconds(600);
            properties.setDeviceWindowSeconds(600);
            properties.setDeviceMaxDistinctUsers(1);
            properties.setAuditQueryDefaultLimit(100);
            return properties;
        }

        @Bean
        WaitingQueueService waitingQueueService() {
            return mock(WaitingQueueService.class);
        }

        @Bean
        PushNotifier pushNotifier() {
            return mock(PushNotifier.class);
        }

        @Bean
        ConcertReadCacheEvictor concertReadCacheEvictor() {
            return mock(ConcertReadCacheEvictor.class);
        }
    }

    @jakarta.annotation.Resource
    private ReservationLifecycleService reservationLifecycleService;

    @jakarta.annotation.Resource
    private AbuseAuditService abuseAuditService;

    @jakarta.annotation.Resource
    private ReservationRepository reservationRepository;

    @jakarta.annotation.Resource
    private SalesPolicyRepository salesPolicyRepository;

    @jakarta.annotation.Resource
    private AbuseAuditLogRepository abuseAuditLogRepository;

    @jakarta.annotation.Resource
    private AdminRefundAuditLogRepository adminRefundAuditLogRepository;

    @jakarta.annotation.Resource
    private SeatRepository seatRepository;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @jakarta.annotation.Resource
    private AgencyRepository agencyRepository;

    @jakarta.annotation.Resource
    private ArtistRepository artistRepository;

    @jakarta.annotation.Resource
    private ConcertRepository concertRepository;

    @jakarta.annotation.Resource
    private ConcertOptionRepository concertOptionRepository;

    @jakarta.annotation.Resource
    private WaitingQueueService waitingQueueService;

    @jakarta.annotation.Resource
    private PushNotifier pushNotifier;

    @jakarta.annotation.Resource
    private PaymentService paymentService;

    @Test
    void holdToPayingToConfirmed_shouldReserveSeat() {
        User user = userRepository.save(new User("step9-integration-user-" + System.nanoTime()));
        Seat seat = saveSeat("A-101");

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );
        assertThat(hold.getStatus()).isEqualTo(Reservation.ReservationStatus.HOLD.name());
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
                .isEqualTo(Seat.SeatStatus.TEMP_RESERVED);

        ReservationLifecycleResponse paying = reservationLifecycleService.startPaying(hold.getId(), user.getId());
        assertThat(paying.getStatus()).isEqualTo(Reservation.ReservationStatus.PAYING.name());

        ReservationLifecycleResponse confirmed = reservationLifecycleService.confirm(hold.getId(), user.getId());
        assertThat(confirmed.getStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED.name());
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
                .isEqualTo(Seat.SeatStatus.RESERVED);
        assertThat(paymentService.getWalletBalance(user.getId())).isEqualTo(100_000L);
        verify(pushNotifier).sendSeatMapStatus(
                eq(seat.getConcertOption().getId()),
                eq(seat.getId()),
                eq(Reservation.ReservationStatus.HOLD.name()),
                eq(user.getId()),
                any()
        );
        verify(pushNotifier).sendSeatMapStatus(
                eq(seat.getConcertOption().getId()),
                eq(seat.getId()),
                eq(Reservation.ReservationStatus.CONFIRMED.name()),
                eq(user.getId()),
                isNull()
        );
    }

    @Test
    void expireTimedOutHolds_shouldExpireAndReleaseSeat() throws Exception {
        User user = userRepository.save(new User("step9-expire-user-" + System.nanoTime()));
        Seat seat = saveSeat("A-102");

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );

        reservationRepository.updateHoldExpiresAt(hold.getId(), LocalDateTime.now().minusSeconds(1));
        int expiredCount = reservationLifecycleService.expireTimedOutHolds();

        Reservation expired = reservationRepository.findById(hold.getId()).orElseThrow();
        assertThat(expiredCount).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(Reservation.ReservationStatus.EXPIRED);
        assertThat(expired.getExpiredAt()).isNotNull();
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
                .isEqualTo(Seat.SeatStatus.AVAILABLE);
        verify(pushNotifier).sendReservationStatus(user.getId(), seat.getId(), Reservation.ReservationStatus.EXPIRED.name());
        verify(pushNotifier).sendSeatMapStatus(
                eq(seat.getConcertOption().getId()),
                eq(seat.getId()),
                eq(Seat.SeatStatus.AVAILABLE.name()),
                isNull(),
                isNull()
        );
    }

    @Test
    void cancelConfirmed_shouldReleaseSeatAndActivateWaitingUser() {
        User user = userRepository.save(new User("step10-cancel-user-" + System.nanoTime()));
        Seat seat = saveSeat("A-103");
        Long concertId = seat.getConcertOption().getConcert().getId();

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );
        reservationLifecycleService.startPaying(hold.getId(), user.getId());
        reservationLifecycleService.confirm(hold.getId(), user.getId());

        when(waitingQueueService.activateUsers(concertId, 1)).thenReturn(List.of(999L));
        when(waitingQueueService.getActiveTtlSeconds(999L)).thenReturn(240L);

        ReservationLifecycleResponse cancelled = reservationLifecycleService.cancel(hold.getId(), user.getId());

        assertThat(cancelled.getStatus()).isEqualTo(Reservation.ReservationStatus.CANCELLED.name());
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getResaleActivatedUserIds()).containsExactly(999L);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
                .isEqualTo(Seat.SeatStatus.AVAILABLE);

        verify(waitingQueueService).activateUsers(concertId, 1);
        verify(pushNotifier).sendQueueActivated(eq(999L), eq(concertId), any());
        verify(pushNotifier).sendSeatMapStatus(
                eq(seat.getConcertOption().getId()),
                eq(seat.getId()),
                eq(Seat.SeatStatus.AVAILABLE.name()),
                isNull(),
                isNull()
        );
    }

    @Test
    void refundCancelled_shouldSetRefundedStatus() {
        User user = userRepository.save(new User("step10-refund-user-" + System.nanoTime()));
        Seat seat = saveSeat("A-104");
        Long concertId = seat.getConcertOption().getConcert().getId();

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );
        reservationLifecycleService.startPaying(hold.getId(), user.getId());
        reservationLifecycleService.confirm(hold.getId(), user.getId());

        when(waitingQueueService.activateUsers(concertId, 1)).thenReturn(List.of());
        reservationLifecycleService.cancel(hold.getId(), user.getId());
        ReservationLifecycleResponse refunded = reservationLifecycleService.refund(hold.getId(), user.getId());

        assertThat(refunded.getStatus()).isEqualTo(Reservation.ReservationStatus.REFUNDED.name());
        assertThat(refunded.getRefundedAt()).isNotNull();
        assertThat(paymentService.getWalletBalance(user.getId())).isEqualTo(200_000L);
    }

    @Test
    void refundCancelled_shouldFailWhenRefundCutoffPassed() {
        User user = userRepository.save(new User("step10-refund-cutoff-user-" + System.nanoTime()));
        Seat seat = saveSeatWithConcertDate("A-104-2", LocalDateTime.now().plusHours(2));
        Long concertId = seat.getConcertOption().getConcert().getId();

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );
        reservationLifecycleService.startPaying(hold.getId(), user.getId());
        reservationLifecycleService.confirm(hold.getId(), user.getId());
        when(waitingQueueService.activateUsers(concertId, 1)).thenReturn(List.of());
        reservationLifecycleService.cancel(hold.getId(), user.getId());

        assertThatThrownBy(() -> reservationLifecycleService.refund(hold.getId(), user.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refund cutoff passed");
    }

    @Test
    void refundCancelled_shouldAllowAdminOverrideWhenRefundCutoffPassed() {
        User user = userRepository.save(new User("step10-refund-cutoff-owner-" + System.nanoTime()));
        User admin = userRepository.save(new User("step10-refund-cutoff-admin-" + System.nanoTime(), UserTier.VIP, UserRole.ADMIN));
        Seat seat = saveSeatWithConcertDate("A-104-3", LocalDateTime.now().plusHours(2));
        Long concertId = seat.getConcertOption().getConcert().getId();

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );
        reservationLifecycleService.startPaying(hold.getId(), user.getId());
        reservationLifecycleService.confirm(hold.getId(), user.getId());
        when(waitingQueueService.activateUsers(concertId, 1)).thenReturn(List.of());
        reservationLifecycleService.cancel(hold.getId(), user.getId());

        ReservationLifecycleResponse refunded = reservationLifecycleService.refundAsAdmin(hold.getId(), admin.getId());

        assertThat(refunded.getStatus()).isEqualTo(Reservation.ReservationStatus.REFUNDED.name());
        assertThat(refunded.getRefundedAt()).isNotNull();
        assertThat(paymentService.getWalletBalance(user.getId())).isEqualTo(200_000L);
        assertThat(adminRefundAuditLogRepository.findAll()).anyMatch(log ->
                log.getReservationId().equals(hold.getId())
                        && log.getActorUserId().equals(admin.getId())
                        && log.getTargetUserId().equals(user.getId())
                        && log.getResult() == AdminRefundAuditLog.AuditResult.SUCCESS
        );
    }

    @Test
    void refundAsAdmin_shouldFailWhenRequesterIsNotAdmin() {
        User user = userRepository.save(new User("step10-refund-non-admin-owner-" + System.nanoTime()));
        User nonAdmin = userRepository.save(new User("step10-refund-non-admin-" + System.nanoTime(), UserTier.BASIC, UserRole.USER));
        Seat seat = saveSeatWithConcertDate("A-104-4", LocalDateTime.now().plusHours(2));
        Long concertId = seat.getConcertOption().getConcert().getId();

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );
        reservationLifecycleService.startPaying(hold.getId(), user.getId());
        reservationLifecycleService.confirm(hold.getId(), user.getId());
        when(waitingQueueService.activateUsers(concertId, 1)).thenReturn(List.of());
        reservationLifecycleService.cancel(hold.getId(), user.getId());

        assertThatThrownBy(() -> reservationLifecycleService.refundAsAdmin(hold.getId(), nonAdmin.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires ADMIN role");
        assertThat(adminRefundAuditLogRepository.findAll()).anyMatch(log ->
                log.getReservationId().equals(hold.getId())
                        && log.getActorUserId().equals(nonAdmin.getId())
                        && log.getResult() == AdminRefundAuditLog.AuditResult.DENIED
        );
    }

    @Test
    void confirmShouldKeepPayingStateWhenWalletIsInsufficient() {
        User user = userRepository.save(new User("step10-insufficient-user-" + System.nanoTime()));
        Seat seat = saveSeat("A-104-1");

        ReservationLifecycleResponse hold = reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), seat.getId())
        );
        reservationLifecycleService.startPaying(hold.getId(), user.getId());

        paymentService.payForReservation(
                user.getId(),
                999_001L,
                200_000L,
                "drain-wallet-before-confirm-" + user.getId()
        );

        assertThatThrownBy(() -> reservationLifecycleService.confirm(hold.getId(), user.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient wallet balance");

        Reservation reservation = reservationRepository.findById(hold.getId()).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.PAYING);
        assertThat(reservation.getHoldExpiresAt()).isNotNull();
        assertThat(reservation.getExpiredAt()).isNull();
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus())
                .isEqualTo(Seat.SeatStatus.TEMP_RESERVED);
        assertThat(paymentService.getWalletBalance(user.getId())).isZero();
    }

    @Test
    void holdInPresale_shouldRejectUserWithInsufficientTier() {
        User basicUser = userRepository.save(new User("step11-basic-user-" + System.nanoTime(), UserTier.BASIC));
        Seat seat = saveSeat("A-105");
        Long concertId = seat.getConcertOption().getConcert().getId();

        LocalDateTime now = LocalDateTime.now();
        salesPolicyRepository.save(SalesPolicy.create(
                seat.getConcertOption().getConcert(),
                now.minusMinutes(1),
                now.plusMinutes(10),
                UserTier.VIP,
                now.plusMinutes(30),
                1
        ));

        assertThatThrownBy(() -> reservationLifecycleService.createHold(new ReservationRequest(basicUser.getId(), seat.getId())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Presale tier not eligible");

        assertThat(reservationRepository.countByUserIdAndConcertIdAndStatusIn(
                basicUser.getId(),
                concertId,
                List.of(Reservation.ReservationStatus.HOLD, Reservation.ReservationStatus.PAYING, Reservation.ReservationStatus.CONFIRMED)
        )).isZero();
    }

    @Test
    void holdShouldEnforcePerUserReservationLimit() {
        User vipUser = userRepository.save(new User("step11-vip-user-" + System.nanoTime(), UserTier.VIP));
        Seat firstSeat = saveSeat("A-106");
        Seat secondSeat = seatRepository.save(new Seat(firstSeat.getConcertOption(), "A-107"));

        LocalDateTime now = LocalDateTime.now();
        salesPolicyRepository.save(SalesPolicy.create(
                firstSeat.getConcertOption().getConcert(),
                null,
                null,
                null,
                now.minusMinutes(1),
                1
        ));

        ReservationLifecycleResponse firstHold = reservationLifecycleService.createHold(
                new ReservationRequest(vipUser.getId(), firstSeat.getId())
        );
        assertThat(firstHold.getStatus()).isEqualTo(Reservation.ReservationStatus.HOLD.name());

        assertThatThrownBy(() -> reservationLifecycleService.createHold(
                new ReservationRequest(vipUser.getId(), secondSeat.getId())
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Per-user reservation limit exceeded");
    }

    @Test
    void holdShouldBlockWhenRateLimitExceeded() {
        User user = userRepository.save(new User("step12-rate-user-" + System.nanoTime(), UserTier.BASIC));
        Seat firstSeat = saveSeat("A-201");

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            abuseAuditLogRepository.save(
                    AbuseAuditLog.allowedHold(
                            user,
                            firstSeat,
                            new ReservationRequest(user.getId(), firstSeat.getId(), "seed-rate-" + i, "device-rate"),
                            null,
                            now.minusSeconds(1)
                    )
            );
        }

        assertThatThrownBy(() -> reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), firstSeat.getId(), "rate-new", "device-rate")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Rate limit exceeded");

        assertThat(abuseAuditLogRepository.countByActionAndUserIdAndOccurredAtAfter(
                AbuseAuditLog.AuditAction.HOLD_CREATE,
                user.getId(),
                LocalDateTime.now().minusMinutes(5)
        )).isGreaterThanOrEqualTo(6);
    }

    @Test
    void holdShouldBlockWhenRequestFingerprintIsDuplicated() {
        User user = userRepository.save(new User("step12-dup-user-" + System.nanoTime(), UserTier.BASIC));
        Seat firstSeat = saveSeat("A-204");
        Seat secondSeat = seatRepository.save(new Seat(firstSeat.getConcertOption(), "A-205"));

        reservationLifecycleService.createHold(new ReservationRequest(user.getId(), firstSeat.getId(), "dup-req-1", "device-dup"));

        assertThatThrownBy(() -> reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), secondSeat.getId(), "dup-req-1", "device-dup")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate request fingerprint detected");
    }

    @Test
    void holdShouldBlockWhenDeviceFingerprintIsSharedAcrossAccounts() {
        User userA = userRepository.save(new User("step12-device-user-a-" + System.nanoTime(), UserTier.BASIC));
        User userB = userRepository.save(new User("step12-device-user-b-" + System.nanoTime(), UserTier.BASIC));
        Seat firstSeat = saveSeat("A-206");
        Seat secondSeat = seatRepository.save(new Seat(firstSeat.getConcertOption(), "A-207"));

        reservationLifecycleService.createHold(new ReservationRequest(userA.getId(), firstSeat.getId(), "dev-req-a", "shared-device-1"));

        assertThatThrownBy(() -> reservationLifecycleService.createHold(
                new ReservationRequest(userB.getId(), secondSeat.getId(), "dev-req-b", "shared-device-1")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Device fingerprint used by multiple accounts");
    }

    @Test
    void abuseAuditQuery_shouldReturnBlockedEvent() {
        User user = userRepository.save(new User("step12-audit-user-" + System.nanoTime(), UserTier.BASIC));
        Seat firstSeat = saveSeat("A-208");
        Seat secondSeat = seatRepository.save(new Seat(firstSeat.getConcertOption(), "A-209"));

        reservationLifecycleService.createHold(new ReservationRequest(user.getId(), firstSeat.getId(), "audit-dup-1", "audit-device"));
        assertThatThrownBy(() -> reservationLifecycleService.createHold(
                new ReservationRequest(user.getId(), secondSeat.getId(), "audit-dup-1", "audit-device")
        )).isInstanceOf(IllegalStateException.class);

        List<AbuseAuditLog> blockedLogs = abuseAuditService.getAuditLogs(
                AbuseAuditLog.AuditAction.HOLD_CREATE,
                AbuseAuditLog.AuditResult.BLOCKED,
                AbuseAuditLog.AuditReason.DUPLICATE_REQUEST_FINGERPRINT,
                user.getId(),
                null,
                null,
                null,
                10
        );
        assertThat(blockedLogs).isNotEmpty();
        assertThat(blockedLogs.get(0).getReason()).isEqualTo(AbuseAuditLog.AuditReason.DUPLICATE_REQUEST_FINGERPRINT);
    }

    private Seat saveSeat(String seatNo) {
        return saveSeatWithConcertDate(seatNo, LocalDateTime.now().plusDays(2));
    }

    private Seat saveSeatWithConcertDate(String seatNo, LocalDateTime concertDate) {
        Agency agency = agencyRepository.save(new Agency("agency-" + seatNo + "-" + System.nanoTime()));
        Artist artist = artistRepository.save(new Artist("artist-" + seatNo + "-" + System.nanoTime(), agency));
        Concert concert = concertRepository.save(new Concert("concert-" + seatNo + "-" + System.nanoTime(), artist));
        ConcertOption option = concertOptionRepository.save(new ConcertOption(concert, concertDate));
        return seatRepository.save(new Seat(option, seatNo));
    }
}
