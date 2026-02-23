package com.ticketrush.application.catalog.model;

import com.ticketrush.domain.artist.Artist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistResult {
    private Long id;
    private String name;
    private String displayName;
    private String genre;
    private LocalDate debutDate;
    private Long entertainmentId;
    private String entertainmentName;

    public static ArtistResult from(Artist artist) {
        return new ArtistResult(
                artist.getId(),
                artist.getName(),
                artist.getDisplayName(),
                artist.getGenre(),
                artist.getDebutDate(),
                artist.getEntertainment() == null ? null : artist.getEntertainment().getId(),
                artist.getEntertainment() == null ? null : artist.getEntertainment().getName()
        );
    }
}
