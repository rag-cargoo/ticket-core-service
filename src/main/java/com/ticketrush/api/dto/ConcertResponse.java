package com.ticketrush.api.dto;

import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.service.ConcertSaleSnapshot;
import com.ticketrush.domain.concert.service.ConcertSaleStatus;
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
    private String agencyName;
    private String agencyCountryCode;
    private String agencyHomepageUrl;
    private ConcertSaleStatus saleStatus;
    private LocalDateTime saleOpensAt;
    private Long saleOpensInSeconds;
    private boolean reservationButtonVisible;
    private boolean reservationButtonEnabled;
    private Long availableSeatCount;
    private Long totalSeatCount;

    public static ConcertResponse from(Concert concert) {
        return from(concert, null);
    }

    public static ConcertResponse from(Concert concert, ConcertSaleSnapshot saleSnapshot) {
        var artist = concert.getArtist();
        var agency = artist.getAgency();

        ConcertSaleSnapshot resolved = saleSnapshot != null
                ? saleSnapshot
                : new ConcertSaleSnapshot(ConcertSaleStatus.UNSCHEDULED, null, null, false, false, 0L, 0L);

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
                resolved.getSaleStatus(),
                resolved.getSaleOpensAt(),
                resolved.getSaleOpensInSeconds(),
                resolved.isReservationButtonVisible(),
                resolved.isReservationButtonEnabled(),
                resolved.getAvailableSeatCount(),
                resolved.getTotalSeatCount()
        );
    }
}
