package com.ticketrush.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Admin/Test] 공연 및 좌석 일괄 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertSetupRequest {
    private String title;
    private String artistName;
    private String artistDisplayName;
    private String artistGenre;
    private LocalDate artistDebutDate;
    private String entertainmentName;
    private String entertainmentCountryCode;
    private String entertainmentHomepageUrl;
    private String promoterName;
    private String promoterCountryCode;
    private String promoterHomepageUrl;
    private String venueName;
    private String venueCity;
    private String venueCountryCode;
    private String venueAddress;
    private LocalDateTime concertDate;
    private int seatCount;
}
