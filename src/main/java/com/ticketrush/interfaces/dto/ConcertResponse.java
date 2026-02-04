package com.ticketrush.interfaces.dto;

import com.ticketrush.domain.concert.entity.Concert;

public record ConcertResponse(
        Long id,
        String title,
        String artist) {
    public static ConcertResponse from(Concert concert) {
        return new ConcertResponse(concert.getId(), concert.getTitle(), concert.getArtist().getName());
    }
}
