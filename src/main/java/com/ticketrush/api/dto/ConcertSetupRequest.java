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
    private String agencyName;
    private String agencyCountryCode;
    private String agencyHomepageUrl;
    private LocalDateTime concertDate;
    private int seatCount;
}
