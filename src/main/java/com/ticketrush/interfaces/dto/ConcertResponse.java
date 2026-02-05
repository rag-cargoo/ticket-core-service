package com.ticketrush.interfaces.dto;

import com.ticketrush.domain.concert.entity.Concert;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertResponse {
    private Long id;
    private String title;
    private String artistName;

    public static ConcertResponse from(Concert concert) {
        return new ConcertResponse(concert.getId(), concert.getTitle(), concert.getArtist().getName());
    }
}