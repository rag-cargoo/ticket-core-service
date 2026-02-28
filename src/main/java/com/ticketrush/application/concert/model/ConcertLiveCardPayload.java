package com.ticketrush.application.concert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ConcertLiveCardPayload {

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

    public static ConcertLiveCardPayload from(ConcertResult concert, ConcertCardRuntimeSnapshot runtimeSnapshot) {
        ConcertCardRuntimeSnapshot runtime = runtimeSnapshot == null ? ConcertCardRuntimeSnapshot.unscheduled() : runtimeSnapshot;
        return ConcertLiveCardPayload.builder()
                .id(concert.getId())
                .title(concert.getTitle())
                .artistName(concert.getArtistName())
                .artistId(concert.getArtistId())
                .artistDisplayName(concert.getArtistDisplayName())
                .artistGenre(concert.getArtistGenre())
                .artistDebutDate(concert.getArtistDebutDate())
                .entertainmentName(concert.getEntertainmentName())
                .entertainmentCountryCode(concert.getEntertainmentCountryCode())
                .entertainmentHomepageUrl(concert.getEntertainmentHomepageUrl())
                .youtubeVideoUrl(concert.getYoutubeVideoUrl())
                .thumbnailUrl(concert.getThumbnailUrl())
                .promoterId(concert.getPromoterId())
                .promoterName(concert.getPromoterName())
                .promoterCountryCode(concert.getPromoterCountryCode())
                .promoterHomepageUrl(concert.getPromoterHomepageUrl())
                .saleStatus(runtime.getSaleStatus())
                .saleOpensAt(runtime.getSaleOpensAt())
                .saleOpensInSeconds(runtime.getSaleOpensInSeconds())
                .reservationButtonVisible(runtime.isReservationButtonVisible())
                .reservationButtonEnabled(runtime.isReservationButtonEnabled())
                .availableSeatCount(runtime.getAvailableSeatCount())
                .totalSeatCount(runtime.getTotalSeatCount())
                .build();
    }
}
