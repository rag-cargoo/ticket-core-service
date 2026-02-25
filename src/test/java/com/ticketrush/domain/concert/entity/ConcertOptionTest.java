package com.ticketrush.domain.concert.entity;

import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.entertainment.Entertainment;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConcertOptionTest {

    @Test
    void constructorShouldUseDefaultMaxSeatsPerOrderWhenNull() {
        ConcertOption option = new ConcertOption(buildConcert("default"), LocalDateTime.now().plusDays(1));

        assertThat(option.getMaxSeatsPerOrder()).isEqualTo(2);
    }

    @Test
    void constructorShouldApplyProvidedMaxSeatsPerOrder() {
        ConcertOption option = new ConcertOption(
                buildConcert("custom"),
                LocalDateTime.now().plusDays(1),
                null,
                null,
                4
        );

        assertThat(option.getMaxSeatsPerOrder()).isEqualTo(4);
    }

    @Test
    void updateScheduleShouldApplyMaxSeatsPerOrderWhenProvided() {
        ConcertOption option = new ConcertOption(
                buildConcert("update"),
                LocalDateTime.now().plusDays(1),
                null,
                null,
                2
        );

        option.updateSchedule(LocalDateTime.now().plusDays(2), null, 110000L, 6);

        assertThat(option.getMaxSeatsPerOrder()).isEqualTo(6);
        assertThat(option.getTicketPriceAmount()).isEqualTo(110000L);
    }

    @Test
    void constructorShouldRejectMaxSeatsPerOrderLessThanOne() {
        assertThatThrownBy(() -> new ConcertOption(
                buildConcert("min"),
                LocalDateTime.now().plusDays(1),
                null,
                null,
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxSeatsPerOrder must be greater than or equal to 1");
    }

    @Test
    void constructorShouldRejectMaxSeatsPerOrderGreaterThanAllowed() {
        assertThatThrownBy(() -> new ConcertOption(
                buildConcert("max"),
                LocalDateTime.now().plusDays(1),
                null,
                null,
                11
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxSeatsPerOrder must be less than or equal to 10");
    }

    private Concert buildConcert(String suffix) {
        Entertainment entertainment = new Entertainment("ent-" + suffix + "-" + System.nanoTime());
        Artist artist = new Artist("artist-" + suffix + "-" + System.nanoTime(), entertainment);
        return new Concert("concert-" + suffix + "-" + System.nanoTime(), artist);
    }
}
