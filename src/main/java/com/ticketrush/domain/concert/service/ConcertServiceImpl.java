package com.ticketrush.domain.concert.service;

import com.ticketrush.domain.agency.Agency;
import com.ticketrush.domain.agency.AgencyRepository;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertServiceImpl implements ConcertService {

    private final ConcertRepository concertRepository;
    private final ConcertOptionRepository concertOptionRepository;
    private final SeatRepository seatRepository;
    private final AgencyRepository agencyRepository;
    private final ArtistRepository artistRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Concert> getConcerts() {
        return concertRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConcertOption> getConcertOptions(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        return concert.getOptions();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> getAvailableSeats(Long concertOptionId) {
        return seatRepository.findByConcertOptionIdAndStatus(concertOptionId, Seat.SeatStatus.AVAILABLE);
    }

    @Override
    @Transactional
    public Concert createConcert(String title, String artistName, String agencyName) {
        Agency agency = agencyRepository.findByName(agencyName)
                .orElseGet(() -> agencyRepository.save(new Agency(agencyName)));
        
        Artist artist = artistRepository.findByName(artistName)
                .orElseGet(() -> artistRepository.save(new Artist(artistName, agency)));
        
        return concertRepository.save(new Concert(title, artist));
    }

    @Override
    @Transactional
    public ConcertOption addOption(Long concertId, LocalDateTime date) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        return concertOptionRepository.save(new ConcertOption(concert, date));
    }

    @Override
    @Transactional
    public void createSeats(Long optionId, int count) {
        ConcertOption option = concertOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Concert option not found"));
        for (int i = 1; i <= count; i++) {
            seatRepository.save(new Seat(option, "A-" + i));
        }
    }

    @Override
    @Transactional
    public void deleteConcert(Long concertId) {
        concertRepository.deleteById(concertId);
    }

    @Override
    @Transactional(readOnly = true)
    public Seat getSeat(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
    }

    @Override
    @Transactional
    public Seat getSeatWithPessimisticLock(Long seatId) {
        return seatRepository.findByIdWithPessimisticLock(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
    }
}
