package com.ticketrush.domain.concert.service;

import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ConcertService {
    List<Concert> getConcerts();
    default Page<Concert> searchConcerts(String keyword, String artistName, Pageable pageable) {
        return searchConcerts(keyword, artistName, null, pageable);
    }
    Page<Concert> searchConcerts(String keyword, String artistName, String agencyName, Pageable pageable);
    List<ConcertOption> getConcertOptions(Long concertId);
    List<Seat> getAvailableSeats(Long concertOptionId);
    
    // Admin & Test Setup
    default Concert createConcert(String title, String artistName, String agencyName) {
        return createConcert(title, artistName, agencyName, null, null, null, null, null);
    }
    Concert createConcert(String title,
                          String artistName,
                          String agencyName,
                          String artistDisplayName,
                          String artistGenre,
                          LocalDate artistDebutDate,
                          String agencyCountryCode,
                          String agencyHomepageUrl);
    ConcertOption addOption(Long concertId, java.time.LocalDateTime date);
    void createSeats(Long optionId, int count);

    void deleteConcert(Long concertId);

    // ReservationService에서 사용할 메서드
    Seat getSeat(Long seatId);
    Seat getSeatWithPessimisticLock(Long seatId);
}
