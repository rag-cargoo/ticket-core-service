package com.ticketrush.domain.concert.service;

import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;

import java.util.List;

public interface ConcertService {
    List<Concert> getConcerts();
    List<ConcertOption> getConcertOptions(Long concertId);
    List<Seat> getAvailableSeats(Long concertOptionId);
    
    // ReservationService에서 사용할 메서드
    Seat getSeat(Long seatId);
}