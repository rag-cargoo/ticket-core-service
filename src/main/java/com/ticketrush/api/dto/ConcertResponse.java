package com.ticketrush.api.dto;

import com.ticketrush.application.concert.model.ConcertCardRuntimeSnapshot;
import com.ticketrush.application.concert.model.ConcertResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String entertainmentName;
    private String entertainmentCountryCode;
    private String entertainmentHomepageUrl;
    private String youtubeVideoUrl;
    private String thumbnailUrl;
    private Long promoterId;
    private String promoterName;
    private String promoterCountryCode;
    private String promoterHomepageUrl;
    private String saleStatus;
    private LocalDateTime saleOpensAt;
    private Long saleOpensInSeconds;
    private boolean reservationButtonVisible;
    private boolean reservationButtonEnabled;
    private long availableSeatCount;
    private long totalSeatCount;

    public static ConcertResponse from(ConcertResult concert) {
        return from(concert, ConcertCardRuntimeSnapshot.unscheduled());
    }

    public static ConcertResponse from(ConcertResult concert, ConcertCardRuntimeSnapshot runtimeSnapshot) {
        ConcertCardRuntimeSnapshot runtime = runtimeSnapshot == null ? ConcertCardRuntimeSnapshot.unscheduled() : runtimeSnapshot;
        return new ConcertResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getArtistName(),
                concert.getArtistId(),
                concert.getArtistDisplayName(),
                concert.getArtistGenre(),
                concert.getArtistDebutDate(),
                concert.getEntertainmentName(),
                concert.getEntertainmentCountryCode(),
                concert.getEntertainmentHomepageUrl(),
                concert.getYoutubeVideoUrl(),
                concert.getThumbnailUrl(),
                concert.getPromoterId(),
                concert.getPromoterName(),
                concert.getPromoterCountryCode(),
                concert.getPromoterHomepageUrl(),
                runtime.getSaleStatus(),
                runtime.getSaleOpensAt(),
                runtime.getSaleOpensInSeconds(),
                runtime.isReservationButtonVisible(),
                runtime.isReservationButtonEnabled(),
                runtime.getAvailableSeatCount(),
                runtime.getTotalSeatCount()
        );
    }
}
