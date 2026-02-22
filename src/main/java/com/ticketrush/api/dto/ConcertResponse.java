package com.ticketrush.api.dto;

import com.ticketrush.domain.concert.entity.Concert;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneOffset;

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
    private String youtubeVideoUrl;
    private String thumbnailUrl;
    private Long promoterId;
    private String promoterName;
    private String promoterCountryCode;
    private String promoterHomepageUrl;

    public static ConcertResponse from(Concert concert) {
        var artist = concert.getArtist();
        var agency = artist.getAgency();
        var promoter = concert.getPromoter();
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
                agency != null ? agency.getHomepageUrl() : null,
                concert.getYoutubeVideoUrl(),
                resolveThumbnailUrl(concert),
                promoter != null ? promoter.getId() : null,
                promoter != null ? promoter.getName() : null,
                promoter != null ? promoter.getCountryCode() : null,
                promoter != null ? promoter.getHomepageUrl() : null
        );
    }

    private static String resolveThumbnailUrl(Concert concert) {
        if (!concert.hasThumbnail()) {
            return null;
        }
        String base = "/api/concerts/" + concert.getId() + "/thumbnail";
        if (concert.getThumbnailUpdatedAt() == null) {
            return base;
        }
        long ts = concert.getThumbnailUpdatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        return base + "?ts=" + ts;
    }
}
