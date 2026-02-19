package com.ticketrush.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminConcertUpsertRequest {
    private String title;
    private String artistName;
    private String agencyName;
    private String artistDisplayName;
    private String artistGenre;
    private LocalDate artistDebutDate;
    private String agencyCountryCode;
    private String agencyHomepageUrl;
    private String youtubeVideoUrl;
}
