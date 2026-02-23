package com.ticketrush.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.ticketrush", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyArchTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_api_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.api..");

    @ArchTest
    static final ArchRule global_should_not_depend_on_api_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.global..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.api..");

    @ArchTest
    static final ArchRule global_should_not_depend_on_infrastructure_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.global..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.application..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.infrastructure..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_api_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.api..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_global_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.global..");

    @ArchTest
    static final ArchRule api_layer_should_not_depend_on_global_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.api..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.global..");

    @ArchTest
    static final ArchRule api_layer_should_not_depend_on_infrastructure_messaging =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.api..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.infrastructure.messaging..");

    @ArchTest
    static final ArchRule api_layer_should_not_depend_on_application_outbound_ports =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.api..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.application.port.outbound..");

    @ArchTest
    static final ArchRule infrastructure_should_not_depend_on_global_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.global..");

    @ArchTest
    static final ArchRule waiting_queue_runtime_should_not_depend_on_waiting_queue_properties_concrete =
            noClasses()
                    .that().resideInAnyPackage(
                            "com.ticketrush.global.sse..",
                            "com.ticketrush.global.scheduler..",
                            "com.ticketrush.global.interceptor..",
                            "com.ticketrush.global.push.."
                    )
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.global.config.WaitingQueueProperties");

    @ArchTest
    static final ArchRule application_should_not_depend_on_spring_redis_directly =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework.data.redis..");

    @ArchTest
    static final ArchRule global_should_not_depend_on_spring_redis_directly =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.global..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework.data.redis..");

    @ArchTest
    static final ArchRule global_should_not_depend_on_spring_kafka_directly =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.global..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework.kafka..");

    @ArchTest
    static final ArchRule global_push_should_not_depend_on_infrastructure_messaging =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.global.push..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.infrastructure.messaging..");

    @ArchTest
    static final ArchRule kafka_websocket_push_notifier_should_not_depend_on_websocket_push_notifier_concrete =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.global.push.KafkaWebSocketPushNotifier")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.global.push.WebSocketPushNotifier");

    @ArchTest
    static final ArchRule waiting_queue_scheduler_should_not_depend_on_push_notifier_concrete =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.global.scheduler.WaitingQueueScheduler")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.ticketrush.global.push..", "com.ticketrush.global.sse..");

    @ArchTest
    static final ArchRule seat_soft_lock_service_should_not_depend_on_transport_specific_push_ports =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.application.reservation.service.SeatSoftLockServiceImpl")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.SsePushPort")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.WebSocketEventDispatchPort")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.WebSocketSubscriptionPort");

    @ArchTest
    static final ArchRule pg_ready_webhook_service_should_not_depend_on_transport_specific_push_ports =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.application.payment.webhook.PgReadyWebhookService")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.SsePushPort")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.WebSocketEventDispatchPort")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.WebSocketSubscriptionPort");

    @ArchTest
    static final ArchRule kafka_reservation_consumer_should_not_depend_on_transport_specific_push_ports =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.infrastructure.messaging.KafkaReservationConsumer")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.SsePushPort")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.WebSocketEventDispatchPort")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.WebSocketSubscriptionPort");

    @ArchTest
    static final ArchRule reservation_lifecycle_service_should_not_depend_on_transport_specific_push_ports =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.application.reservation.service.ReservationLifecycleServiceImpl")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.SsePushPort")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.WebSocketEventDispatchPort")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.WebSocketSubscriptionPort");

    @ArchTest
    static final ArchRule kafka_push_event_consumer_should_not_depend_on_transport_specific_push_ports =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.infrastructure.messaging.KafkaPushEventConsumer")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.SsePushPort")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.port.outbound.WebSocketSubscriptionPort");

    @ArchTest
    static final ArchRule rest_controllers_should_not_depend_on_repository_directly =
            noClasses()
                    .that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule rest_controllers_should_not_depend_on_application_service_package_directly =
            noClasses()
                    .that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.application..service..");

    @ArchTest
    static final ArchRule payment_webhook_controller_should_not_depend_on_pg_ready_webhook_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.controller.PaymentWebhookController")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.payment.webhook.PgReadyWebhookService");

    @ArchTest
    static final ArchRule waiting_queue_scheduler_should_not_depend_on_waiting_queue_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.global.scheduler.WaitingQueueScheduler")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.waitingqueue.service.WaitingQueueService");

    @ArchTest
    static final ArchRule reservation_lifecycle_scheduler_should_not_depend_on_reservation_lifecycle_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.global.scheduler.ReservationLifecycleScheduler")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.reservation.service.ReservationLifecycleService");

    @ArchTest
    static final ArchRule redisson_lock_facade_should_not_depend_on_reservation_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.global.lock.RedissonLockFacade")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.reservation.service.ReservationService");

    @ArchTest
    static final ArchRule wallet_payment_gateway_should_not_depend_on_payment_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.infrastructure.payment.gateway.WalletPaymentGateway")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.payment.service.PaymentService");

    @ArchTest
    static final ArchRule reservation_waiting_queue_adapter_should_not_depend_on_waiting_queue_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.infrastructure.reservation.adapter.outbound.ReservationWaitingQueuePortAdapter")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.waitingqueue.service.WaitingQueueService");

    @ArchTest
    static final ArchRule kafka_reservation_consumer_should_not_depend_on_reservation_services_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.infrastructure.messaging.KafkaReservationConsumer")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.reservation.service.ReservationService")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.reservation.service.ReservationQueueService");

    @ArchTest
    static final ArchRule jwt_authentication_filter_should_not_depend_on_jwt_token_provider_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.infrastructure.auth.security.JwtAuthenticationFilter")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.auth.service.JwtTokenProvider");

    @ArchTest
    static final ArchRule reservation_controller_should_not_depend_on_domain_reservation_services =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.controller.ReservationController")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.domain.reservation..");

    @ArchTest
    static final ArchRule reservation_controller_should_not_depend_on_reservation_queue_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.controller.ReservationController")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.reservation.service.ReservationQueueService");

    @ArchTest
    static final ArchRule waiting_queue_controller_should_not_depend_on_waiting_queue_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.waitingqueue.WaitingQueueController")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.waitingqueue.service.WaitingQueueService");

    @ArchTest
    static final ArchRule realtime_controllers_should_not_depend_on_realtime_subscription_service_directly =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.controller.ReservationController")
                    .or().haveFullyQualifiedName("com.ticketrush.api.waitingqueue.WaitingQueueController")
                    .or().haveFullyQualifiedName("com.ticketrush.api.controller.WebSocketPushController")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.ticketrush.application.realtime.service.RealtimeSubscriptionService");

    @ArchTest
    static final ArchRule catalog_controllers_should_not_depend_on_domain_catalog_services =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.controller.EntertainmentController")
                    .or().haveFullyQualifiedName("com.ticketrush.api.controller.EntertainmentCatalogController")
                    .or().haveFullyQualifiedName("com.ticketrush.api.controller.ArtistController")
                    .or().haveFullyQualifiedName("com.ticketrush.api.controller.PromoterController")
                    .or().haveFullyQualifiedName("com.ticketrush.api.controller.VenueController")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.ticketrush.domain.entertainment.service..",
                            "com.ticketrush.domain.artist.service..",
                            "com.ticketrush.domain.promoter.service..",
                            "com.ticketrush.domain.venue.service.."
                    );

    @ArchTest
    static final ArchRule auth_and_user_controllers_should_not_depend_on_domain_auth_user_services =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.controller.AuthController")
                    .or().haveFullyQualifiedName("com.ticketrush.api.controller.SocialAuthController")
                    .or().haveFullyQualifiedName("com.ticketrush.api.controller.UserController")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.ticketrush.domain.auth.service..",
                            "com.ticketrush.domain.user.service.."
                    );

    @ArchTest
    static final ArchRule concert_controllers_should_not_depend_on_domain_concert_services =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.controller.ConcertController")
                    .or().haveFullyQualifiedName("com.ticketrush.api.controller.AdminConcertController")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.domain.concert.service..");

    @ArchTest
    static final ArchRule wallet_controller_should_not_depend_on_domain_payment_services =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.controller.WalletController")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.domain.payment.service..");

    @ArchTest
    static final ArchRule infrastructure_messaging_should_not_depend_on_domain_reservation_events =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.infrastructure.messaging..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.domain.reservation.event..");
}
