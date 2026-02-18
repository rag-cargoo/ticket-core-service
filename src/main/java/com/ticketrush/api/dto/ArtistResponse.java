package com.ticketrush.api.dto;

import com.ticketrush.domain.artist.Artist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistResponse {
    private Long id;
    private String name;
    private String displayName;
    private String genre;
    private LocalDate debutDate;
    private Long agencyId;
    private String agencyName;

    public static ArtistResponse from(Artist artist) {
        return new ArtistResponse(
                artist.getId(),
                artist.getName(),
                artist.getDisplayName(),
                artist.getGenre(),
                artist.getDebutDate(),
                artist.getAgency() == null ? null : artist.getAgency().getId(),
                artist.getAgency() == null ? null : artist.getAgency().getName()
        );
    }
}
