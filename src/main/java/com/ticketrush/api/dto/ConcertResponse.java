package com.ticketrush.api.dto;

import com.ticketrush.domain.concert.entity.Concert;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertResponse {
    private Long id;
    private String title;
    private String artistName;
    private Long artistId;
    private String artistDisplayName;
    private String artistGenre;
    private LocalDate artistDebutDate;
    private String agencyName;
    private String agencyCountryCode;
    private String agencyHomepageUrl;

    public static ConcertResponse from(Concert concert) {
        var artist = concert.getArtist();
        var agency = artist.getAgency();
        return new ConcertResponse(
                concert.getId(),
                concert.getTitle(),
                artist.getName(),
                artist.getId(),
                artist.getDisplayName(),
                artist.getGenre(),
                artist.getDebutDate(),
                agency != null ? agency.getName() : null,
                agency != null ? agency.getCountryCode() : null,
                agency != null ? agency.getHomepageUrl() : null
        );
    }
}
