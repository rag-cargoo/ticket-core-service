package com.ticketrush;

import com.ticketrush.domain.agency.Agency;
import com.ticketrush.domain.agency.AgencyRepository;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.entity.*;
import com.ticketrush.domain.concert.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ConcertRepository concertRepository;
    private final ConcertOptionRepository concertOptionRepository;
    private final SeatRepository seatRepository;
    private final AgencyRepository agencyRepository;
    private final ArtistRepository artistRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (concertRepository.count() > 0) {
            return;
        }

        // 0. Create Agencies
        Agency edam = agencyRepository.save(new Agency("EDAM Entertainment"));
        Agency ador = agencyRepository.save(new Agency("ADOR"));
        Agency bighit = agencyRepository.save(new Agency("BIGHIT MUSIC"));

        // 0. Create Artists
        Artist iu = artistRepository.save(new Artist("IU", edam));
        Artist newjeans = artistRepository.save(new Artist("NewJeans", ador));
        Artist bts = artistRepository.save(new Artist("BTS", bighit));

        // 1. IU Concert
        createConcertWithDetails("The Golden Hour", iu,
                List.of(LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(11)));

        // 2. NewJeans Concert
        createConcertWithDetails("Bunnies Camp", newjeans,
                List.of(LocalDateTime.now().plusDays(20)));

        // 3. BTS Concert
        createConcertWithDetails("Yet To Come", bts,
                List.of(LocalDateTime.now().plusDays(30), LocalDateTime.now().plusDays(31),
                        LocalDateTime.now().plusDays(32)));
    }

    private void createConcertWithDetails(String title, Artist artist, List<LocalDateTime> dates) {
        Concert concert = new Concert(title, artist);
        concertRepository.save(concert);

        for (LocalDateTime date : dates) {
            ConcertOption option = new ConcertOption(concert, date);
            concertOptionRepository.save(option);

            // Create Seats (A-1 to A-50)
            List<Seat> seats = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                seats.add(new Seat(option, "A-" + i));
            }
            seatRepository.saveAll(seats);
        }
    }
}
