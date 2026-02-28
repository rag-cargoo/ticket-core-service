package com.ticketrush.application.concert.service;

import com.ticketrush.application.reservation.model.ReservationCreateCommand;
import com.ticketrush.application.reservation.model.SalesPolicyUpsertCommand;
import com.ticketrush.application.reservation.port.inbound.SalesPolicyUseCase;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.application.reservation.service.ReservationService;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    private SalesPolicyUseCase salesPolicyUseCase;

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
    void searchEndpointIncludesRealtimeMetadataAndRuntimeFields() throws Exception {
        concertService.createConcert("Realtime Concert", "Realtime Artist", "Realtime Entertainment");

        mockMvc.perform(get("/api/concerts/search")
                        .param("keyword", "Realtime")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverNow").isNotEmpty())
                .andExpect(jsonPath("$.realtimeMode").value("websocket"))
                .andExpect(jsonPath("$.hybridPollIntervalMillis").value(30000))
                .andExpect(jsonPath("$.items[0].saleStatus").value("UNSCHEDULED"))
                .andExpect(jsonPath("$.items[0].reservationButtonVisible").value(false))
                .andExpect(jsonPath("$.items[0].reservationButtonEnabled").value(false))
                .andExpect(jsonPath("$.items[0].availableSeatCount").value(0))
                .andExpect(jsonPath("$.items[0].totalSeatCount").value(0));
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
        reservationService.createReservation(new ReservationCreateCommand(user.getId(), selectedSeatId));

        List<Seat> secondRead = concertService.getAvailableSeats(option.getId());
        assertThat(secondRead).hasSize(2);
        assertThat(secondRead).extracting(Seat::getId).doesNotContain(selectedSeatId);
    }

    @Test
    void highlightsEndpointReturnsBackendComputedTopLists() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.save(new User("highlight_user_" + System.nanoTime(), UserTier.VIP));

        var openingSoonConcert = concertService.createConcert(
                "Highlights Soon Concert",
                "Highlights Artist Soon",
                "Highlights Entertainment Soon"
        );
        var openingSoonOption = concertService.addOption(openingSoonConcert.getId(), now.plusDays(1));
        concertService.createSeats(openingSoonOption.getId(), 120);
        salesPolicyUseCase.upsert(
                openingSoonConcert.getId(),
                new SalesPolicyUpsertCommand(null, null, null, now.plusMinutes(15), 4)
        );

        var lowStockConcert = concertService.createConcert(
                "Highlights Low Stock Concert",
                "Highlights Artist Low",
                "Highlights Entertainment Low"
        );
        var lowStockOption = concertService.addOption(lowStockConcert.getId(), now.plusDays(1));
        concertService.createSeats(lowStockOption.getId(), 40);
        salesPolicyUseCase.upsert(
                lowStockConcert.getId(),
                new SalesPolicyUpsertCommand(null, null, null, now.minusMinutes(10), 4)
        );

        List<Seat> lowStockSeats = concertService.getAvailableSeats(lowStockOption.getId());
        for (int index = 0; index < 30; index++) {
            reservationService.createReservation(new ReservationCreateCommand(user.getId(), lowStockSeats.get(index).getId()));
        }

        mockMvc.perform(get("/api/concerts/highlights")
                        .param("openingSoonLimit", "3")
                        .param("sellOutRiskLimit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.openingSoonWithinHours").value(12))
                .andExpect(jsonPath("$.sellOutRiskSeatThreshold").value(30))
                .andExpect(jsonPath("$.sellOutRiskRatioThreshold").value(18))
                .andExpect(content().string(containsString("Highlights Soon Concert")))
                .andExpect(content().string(containsString("Highlights Low Stock Concert")));
    }
}
