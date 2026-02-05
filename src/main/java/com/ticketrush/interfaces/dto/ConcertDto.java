package com.ticketrush.interfaces.dto;

import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;

import java.time.LocalDateTime;

// 공연 조회 응답
public record ConcertResponse(Long id, String title, String artistName) {
    public static ConcertResponse from(Concert concert) {
        return new ConcertResponse(concert.getId(), concert.getTitle(), concert.getArtist().getName());
    }
}

// 공연 옵션(날짜) 조회 응답
public record ConcertOptionResponse(Long id, LocalDateTime concertDate) {
    public static ConcertOptionResponse from(ConcertOption option) {
        return new ConcertOptionResponse(option.getId(), option.getConcertDate());
    }
}

// 좌석 조회 응답
public record SeatResponse(Long id, String seatNumber, String status) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getStatus().name());
    }
}
