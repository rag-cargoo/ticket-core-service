package com.ticketrush.domain.catalog.service;

import com.ticketrush.domain.agency.service.AgencyService;
import com.ticketrush.domain.agency.service.AgencyServiceImpl;
import com.ticketrush.domain.artist.service.ArtistService;
import com.ticketrush.domain.artist.service.ArtistServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({AgencyServiceImpl.class, ArtistServiceImpl.class})
class AgencyArtistCrudDataJpaTest {

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private ArtistService artistService;

    @Test
    void deleteAgency_blocksWhenArtistReferencesIt() {
        var agency = agencyService.create("HYBE", "KR", "https://hybecorp.com");
        artistService.create("BTS", agency.getId(), "BTS", "K-POP", null);

        assertThatThrownBy(() -> agencyService.delete(agency.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("referenced by artists");
    }

    @Test
    void createArtist_andUpdateArtist_worksWithAgencyRelation() {
        var agency = agencyService.create("ADOR", "KR", "https://ador.world");
        var artist = artistService.create("NewJeans", agency.getId(), "NJ", "K-POP", null);

        var updated = artistService.update(
                artist.getId(),
                "NewJeans",
                agency.getId(),
                "NewJeans",
                "Pop",
                null
        );

        assertThat(updated.getAgency().getId()).isEqualTo(agency.getId());
        assertThat(updated.getDisplayName()).isEqualTo("NewJeans");
        assertThat(updated.getGenre()).isEqualTo("Pop");
    }
}
