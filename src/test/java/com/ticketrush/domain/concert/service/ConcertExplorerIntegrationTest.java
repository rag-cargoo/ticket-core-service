package com.ticketrush.domain.concert.service;

import com.ticketrush.api.dto.ReservationRequest;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.reservation.service.ReservationService;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserTier;
import com.ticketrush.global.cache.ConcertCacheNames;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConcertExplorerIntegrationTest {

    @Autowired
    private ConcertService concertService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchEndpointReturnsPagedResponse() throws Exception {
        concertService.createConcert("Alpha Night Tour", "Artist-A", "Entertainment-A");
        concertService.createConcert("Bravo Night Tour", "Artist-B", "Entertainment-B");

        mockMvc.perform(get("/api/concerts/search")
                        .param("keyword", "alpha")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Alpha Night Tour"));
    }

    @Test
    void searchEndpointIncludesArtistEntertainmentExpandedFields() throws Exception {
        concertService.createConcert(
                "Meta Concert",
                "Artist-Meta",
                "Entertainment-Meta",
                "Artist Meta Display",
                "K-POP",
                LocalDate.of(2022, 7, 22),
                "KR",
                "https://entertainment-meta.example.com"
        );

        mockMvc.perform(get("/api/concerts/search")
                        .param("artistName", "Artist-Meta")
                        .param("entertainmentName", "Entertainment-Meta")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "entertainmentName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].artistName").value("Artist-Meta"))
                .andExpect(jsonPath("$.items[0].artistDisplayName").value("Artist Meta Display"))
                .andExpect(jsonPath("$.items[0].artistGenre").value("K-POP"))
                .andExpect(jsonPath("$.items[0].artistDebutDate").value("2022-07-22"))
                .andExpect(jsonPath("$.items[0].entertainmentName").value("Entertainment-Meta"))
                .andExpect(jsonPath("$.items[0].entertainmentCountryCode").value("KR"));
    }

    @Test
    void availableSeatCacheIsEvictedAfterReservation() {
        User user = userRepository.save(new User("cache_user_" + System.nanoTime(), UserTier.BASIC));
        var concert = concertService.createConcert("Cache Invalidation Show", "Cache Artist", "Cache Entertainment");
        var option = concertService.addOption(concert.getId(), LocalDateTime.now().plusDays(1));
        concertService.createSeats(option.getId(), 3);

        List<Seat> firstRead = concertService.getAvailableSeats(option.getId());
        assertThat(firstRead).hasSize(3);

        Cache seatCache = cacheManager.getCache(ConcertCacheNames.CONCERT_AVAILABLE_SEATS);
        assertThat(seatCache).isNotNull();
        assertThat(seatCache.get(option.getId())).isNotNull();

        Long selectedSeatId = firstRead.get(0).getId();
        reservationService.createReservation(new ReservationRequest(user.getId(), selectedSeatId));

        List<Seat> secondRead = concertService.getAvailableSeats(option.getId());
        assertThat(secondRead).hasSize(2);
        assertThat(secondRead).extracting(Seat::getId).doesNotContain(selectedSeatId);
    }
}
