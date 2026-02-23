package com.ticketrush.application.catalog.service;

import com.ticketrush.application.catalog.service.EntertainmentService;
import com.ticketrush.application.catalog.service.EntertainmentServiceImpl;
import com.ticketrush.application.catalog.service.ArtistService;
import com.ticketrush.application.catalog.service.ArtistServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({EntertainmentServiceImpl.class, ArtistServiceImpl.class})
class EntertainmentArtistCrudDataJpaTest {

    @Autowired
    private EntertainmentService entertainmentService;

    @Autowired
    private ArtistService artistService;

    @Test
    void deleteEntertainment_blocksWhenArtistReferencesIt() {
        var entertainment = entertainmentService.create("HYBE", "KR", "https://hybecorp.com");
        artistService.create("BTS", entertainment.getId(), "BTS", "K-POP", null);

        assertThatThrownBy(() -> entertainmentService.delete(entertainment.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("referenced by artists");
    }

    @Test
    void createArtist_andUpdateArtist_worksWithEntertainmentRelation() {
        var entertainment = entertainmentService.create("ADOR", "KR", "https://ador.world");
        var artist = artistService.create("NewJeans", entertainment.getId(), "NJ", "K-POP", null);

        var updated = artistService.update(
                artist.getId(),
                "NewJeans",
                entertainment.getId(),
                "NewJeans",
                "Pop",
                null
        );

        assertThat(updated.getEntertainment().getId()).isEqualTo(entertainment.getId());
        assertThat(updated.getDisplayName()).isEqualTo("NewJeans");
        assertThat(updated.getGenre()).isEqualTo("Pop");
    }
}
