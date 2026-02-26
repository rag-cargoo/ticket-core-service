package com.ticketrush;

import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.entertainment.EntertainmentRepository;
import com.ticketrush.domain.promoter.PromoterRepository;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import com.ticketrush.domain.seed.SeedMarkerRepository;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.venue.VenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DataInitializerDataJpaTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SeedMarkerRepository seedMarkerRepository;
    @Autowired
    private EntertainmentRepository entertainmentRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private PromoterRepository promoterRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private ConcertRepository concertRepository;
    @Autowired
    private ConcertOptionRepository concertOptionRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private SalesPolicyRepository salesPolicyRepository;

    @Test
    @DisplayName("local profile + portfolio enabled 시 샘플 시드가 1회만 적용된다")
    void seedsPortfolioOnlyOnce() {
        DataInitializer initializer = buildInitializer("local", true, "portfolio_seed_marker_test", "local,demo");

        initializer.run();

        long userCountAfterFirst = userRepository.count();
        long markerCountAfterFirst = seedMarkerRepository.count();
        long concertCountAfterFirst = concertRepository.count();
        long optionCountAfterFirst = concertOptionRepository.count();
        long seatCountAfterFirst = seatRepository.count();
        long policyCountAfterFirst = salesPolicyRepository.count();

        assertThat(userRepository.existsByUsername("admin")).isTrue();
        assertThat(markerCountAfterFirst).isEqualTo(1L);
        assertThat(concertCountAfterFirst).isEqualTo(6L);
        assertThat(optionCountAfterFirst).isEqualTo(12L);
        assertThat(seatCountAfterFirst).isEqualTo(216L);
        assertThat(policyCountAfterFirst).isEqualTo(5L);

        initializer.run();

        assertThat(userRepository.count()).isEqualTo(userCountAfterFirst);
        assertThat(seedMarkerRepository.count()).isEqualTo(markerCountAfterFirst);
        assertThat(entertainmentRepository.count()).isEqualTo(6L);
        assertThat(artistRepository.count()).isEqualTo(6L);
        assertThat(promoterRepository.count()).isEqualTo(1L);
        assertThat(venueRepository.count()).isEqualTo(1L);
        assertThat(concertRepository.count()).isEqualTo(concertCountAfterFirst);
        assertThat(concertOptionRepository.count()).isEqualTo(optionCountAfterFirst);
        assertThat(seatRepository.count()).isEqualTo(seatCountAfterFirst);
        assertThat(salesPolicyRepository.count()).isEqualTo(policyCountAfterFirst);
    }

    @Test
    @DisplayName("허용되지 않은 profile에서는 portfolio 시드가 실행되지 않는다")
    void skipsPortfolioSeedOnDisallowedProfile() {
        DataInitializer initializer = buildInitializer("docker", true, "portfolio_seed_marker_test", "local,demo");

        initializer.run();

        assertThat(userRepository.existsByUsername("admin")).isTrue();
        assertThat(seedMarkerRepository.count()).isZero();
        assertThat(entertainmentRepository.count()).isZero();
        assertThat(artistRepository.count()).isZero();
        assertThat(promoterRepository.count()).isZero();
        assertThat(venueRepository.count()).isZero();
        assertThat(concertRepository.count()).isZero();
        assertThat(concertOptionRepository.count()).isZero();
        assertThat(seatRepository.count()).isZero();
        assertThat(salesPolicyRepository.count()).isZero();
    }

    private DataInitializer buildInitializer(
            String activeProfile,
            boolean portfolioEnabled,
            String markerKey,
            String allowedProfiles
    ) {
        DataInitializer initializer = new DataInitializer(
                userRepository,
                seedMarkerRepository,
                entertainmentRepository,
                artistRepository,
                promoterRepository,
                venueRepository,
                concertRepository,
                concertOptionRepository,
                seatRepository,
                salesPolicyRepository,
                new MockEnvironment().withProperty("spring.profiles.active", activeProfile)
        );
        ReflectionTestUtils.setField(initializer, "createAdminUser", true);
        ReflectionTestUtils.setField(initializer, "portfolioSeedEnabled", portfolioEnabled);
        ReflectionTestUtils.setField(initializer, "portfolioSeedMarkerKey", markerKey);
        ReflectionTestUtils.setField(initializer, "portfolioSeedProfiles", allowedProfiles);
        return initializer;
    }
}
