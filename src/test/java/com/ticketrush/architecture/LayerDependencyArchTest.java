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
    static final ArchRule rest_controllers_should_not_depend_on_repository_directly =
            noClasses()
                    .that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule reservation_controller_should_not_depend_on_domain_reservation_services =
            noClasses()
                    .that().haveFullyQualifiedName("com.ticketrush.api.controller.ReservationController")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.domain.reservation..");

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
    static final ArchRule global_messaging_should_not_depend_on_domain_reservation_events =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.global.messaging..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.domain.reservation.event..");
}
