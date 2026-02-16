package com.ticketrush.domain.concert.service;

import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ConcertService {
    List<Concert> getConcerts();
    Page<Concert> searchConcerts(String keyword, String artistName, Pageable pageable);
    List<ConcertOption> getConcertOptions(Long concertId);
    List<Seat> getAvailableSeats(Long concertOptionId);
    
    // Admin & Test Setup
    Concert createConcert(String title, String artistName, String agencyName);
    ConcertOption addOption(Long concertId, java.time.LocalDateTime date);
    void createSeats(Long optionId, int count);

    void deleteConcert(Long concertId);

    // ReservationService에서 사용할 메서드
    Seat getSeat(Long seatId);
    Seat getSeatWithPessimisticLock(Long seatId);
}
