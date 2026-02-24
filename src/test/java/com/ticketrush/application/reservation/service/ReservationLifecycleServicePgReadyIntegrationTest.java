package com.ticketrush.application.reservation.service;

import com.ticketrush.application.concert.port.outbound.ConcertReadCacheEvictPort;
import com.ticketrush.application.payment.service.PaymentMethodCatalogService;
import com.ticketrush.application.port.outbound.QueueRuntimePushPort;
import com.ticketrush.application.port.outbound.ReservationStatusPushPort;
import com.ticketrush.application.port.outbound.SeatMapPushPort;
import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.ReservationLifecycleResult;
import com.ticketrush.application.waitingqueue.service.WaitingQueueService;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.entertainment.Entertainment;
import com.ticketrush.domain.entertainment.EntertainmentRepository;
import com.ticketrush.domain.payment.entity.PaymentTransaction;
import com.ticketrush.domain.payment.entity.PaymentTransactionStatus;
import com.ticketrush.domain.payment.repository.PaymentTransactionRepository;
import com.ticketrush.domain.reservation.entity.Reservation;
import com.ticketrush.domain.reservation.repository.AdminRefundAuditLogRepository;
import com.ticketrush.domain.reservation.repository.AbuseAuditLogRepository;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.global.config.AbuseGuardProperties;
import com.ticketrush.global.config.PaymentProperties;
import com.ticketrush.global.config.ReservationProperties;
import com.ticketrush.infrastructure.payment.gateway.PgReadyPaymentGateway;
import com.ticketrush.infrastructure.reservation.adapter.outbound.ReservationPaymentPortAdapter;
import com.ticketrush.infrastructure.reservation.adapter.outbound.ReservationSeatPortAdapter;
import com.ticketrush.infrastructure.reservation.adapter.outbound.ReservationUserPortAdapter;
import com.ticketrush.infrastructure.reservation.adapter.outbound.ReservationWaitingQueuePortAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
@TestPropertySource(properties = "app.payment.provider=pg-ready")
@Import({
        ReservationLifecycleServiceImpl.class,
        SalesPolicyServiceImpl.class,
        AbuseAuditServiceImpl.class,
        AdminRefundAuditService.class,
        AbuseAuditWriter.class,
        PaymentMethodCatalogService.class,
        PgReadyPaymentGateway.class,
        ReservationSeatPortAdapter.class,
        ReservationPaymentPortAdapter.class,
        ReservationUserPortAdapter.class,
        ReservationWaitingQueuePortAdapter.class,
        ReservationLifecycleServicePgReadyIntegrationTest.TestConfig.class
})
class ReservationLifecycleServicePgReadyIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ReservationProperties reservationProperties() {
            ReservationProperties properties = new ReservationProperties();
            properties.setHoldTtlSeconds(60);
            properties.setExpireCheckDelayMillis(1000);
            properties.setRefundCutoffHoursBeforeConcert(24);
            return properties;
        }

        @Bean
        PaymentProperties paymentProperties() {
            PaymentProperties properties = new PaymentProperties();
            properties.setProvider("pg-ready");
            properties.setExternalLiveEnabled(true);
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

        @Bean(name = {"queuePushNotifier", "queueRuntimePushNotifier"})
        QueueRuntimePushPort queuePushNotifier() {
            return mock(QueueRuntimePushPort.class);
        }

        @Bean(name = "reservationStatusPushNotifier")
        ReservationStatusPushPort reservationStatusPushNotifier() {
            return mock(ReservationStatusPushPort.class);
        }

        @Bean(name = "seatMapPushNotifier")
        SeatMapPushPort seatMapPushNotifier() {
            return mock(SeatMapPushPort.class);
        }

        @Bean
        ConcertReadCacheEvictPort concertReadCacheEvictor() {
            return mock(ConcertReadCacheEvictPort.class);
        }

        @Bean
        WaitingQueueService waitingQueueService() {
            return mock(WaitingQueueService.class);
        }
    }

    @jakarta.annotation.Resource
    private ReservationLifecycleService reservationLifecycleService;

    @jakarta.annotation.Resource
    private PaymentProperties paymentProperties;

    @jakarta.annotation.Resource
    private ReservationRepository reservationRepository;

    @jakarta.annotation.Resource
    private PaymentTransactionRepository paymentTransactionRepository;

    @jakarta.annotation.Resource
    private SeatRepository seatRepository;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @jakarta.annotation.Resource
    private EntertainmentRepository entertainmentRepository;

    @jakarta.annotation.Resource
    private ArtistRepository artistRepository;

    @jakarta.annotation.Resource
    private ConcertRepository concertRepository;

    @jakarta.annotation.Resource
    private ConcertOptionRepository concertOptionRepository;

    @jakarta.annotation.Resource
    private SalesPolicyRepository salesPolicyRepository;

    @jakarta.annotation.Resource
    private AbuseAuditLogRepository abuseAuditLogRepository;

    @jakarta.annotation.Resource
    private AdminRefundAuditLogRepository adminRefundAuditLogRepository;

    @Test
    void confirmShouldReturnRedirectActionWhenPgReadyLiveEnabled() {
        paymentProperties.setExternalLiveEnabled(true);

        User user = userRepository.save(new User("pg-ready-flow-user-" + System.nanoTime()));
        Seat seat = saveSeat("PG-R-101");

        ReservationLifecycleResult hold = reservationLifecycleService.createHold(
                new ReservationCreateCommand(user.getId(), seat.getId())
        );
        reservationLifecycleService.startPaying(hold.getId(), user.getId());

        ReservationLifecycleResult confirmed = reservationLifecycleService.confirm(hold.getId(), user.getId(), "CARD");

        Reservation storedReservation = reservationRepository.findById(hold.getId()).orElseThrow();
        PaymentTransaction paymentTransaction = paymentTransactionRepository.findById(confirmed.getPaymentTransactionId()).orElseThrow();

        assertThat(confirmed.getStatus()).isEqualTo(Reservation.ReservationStatus.PAYING.name());
        assertThat(confirmed.getConfirmedAt()).isNull();
        assertThat(confirmed.getPaymentMethod()).isEqualTo("CARD");
        assertThat(confirmed.getPaymentProvider()).isEqualTo("pg-ready");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(confirmed.getPaymentAction()).isEqualTo("REDIRECT");
        assertThat(confirmed.getPaymentRedirectUrl()).isNotBlank();
        assertThat(confirmed.getPaymentRedirectUrl()).startsWith(paymentProperties.getPgReadyCheckoutBaseUrl());
        assertThat(confirmed.getPaymentRedirectUrl()).contains("reservationId=" + hold.getId());
        assertThat(confirmed.getPaymentRedirectUrl()).contains("paymentMethod=CARD");
        assertThat(confirmed.getPaymentRedirectUrl()).contains("paymentTransactionId=" + paymentTransaction.getId());

        assertThat(storedReservation.getStatus()).isEqualTo(Reservation.ReservationStatus.PAYING);
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(Seat.SeatStatus.TEMP_RESERVED);
        assertThat(paymentTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING);
    }

    @Test
    void confirmShouldWaitWebhookWhenPgReadyLiveDisabled() {
        paymentProperties.setExternalLiveEnabled(false);

        User user = userRepository.save(new User("pg-ready-flow-user-off-" + System.nanoTime()));
        Seat seat = saveSeat("PG-R-102");

        ReservationLifecycleResult hold = reservationLifecycleService.createHold(
                new ReservationCreateCommand(user.getId(), seat.getId())
        );
        reservationLifecycleService.startPaying(hold.getId(), user.getId());

        ReservationLifecycleResult confirmed = reservationLifecycleService.confirm(hold.getId(), user.getId(), "CARD");

        assertThat(confirmed.getStatus()).isEqualTo(Reservation.ReservationStatus.PAYING.name());
        assertThat(confirmed.getPaymentProvider()).isEqualTo("pg-ready");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(confirmed.getPaymentAction()).isEqualTo("WAIT_WEBHOOK");
        assertThat(confirmed.getPaymentRedirectUrl()).isNull();
    }

    private Seat saveSeat(String seatNumber) {
        Entertainment entertainment = entertainmentRepository.save(new Entertainment("pg-ready-entertainment-" + seatNumber + "-" + System.nanoTime()));
        Artist artist = artistRepository.save(new Artist("pg-ready-artist-" + seatNumber + "-" + System.nanoTime(), entertainment));
        Concert concert = concertRepository.save(new Concert("pg-ready-concert-" + seatNumber + "-" + System.nanoTime(), artist));
        ConcertOption option = concertOptionRepository.save(new ConcertOption(concert, LocalDateTime.now().plusDays(4)));
        return seatRepository.save(new Seat(option, seatNumber));
    }
}
