package com.ticketrush.domain.concert.service;

import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;

import java.util.List;

public interface ConcertService {
    List<Concert> getConcerts();
    List<ConcertOption> getConcertOptions(Long concertId);
    List<Seat> getAvailableSeats(Long concertOptionId);
    
    // Admin & Test Setup
    Concert createConcert(String title, String artistName, String agencyName);
    ConcertOption addOption(Long concertId, java.time.LocalDateTime date);
    void createSeats(Long optionId, int count);

    // ReservationService에서 사용할 메서드
    Seat getSeat(Long seatId);
    Seat getSeatWithPessimisticLock(Long seatId);
}