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
    private Long artistId;
    private Long promoterId;
    private String artistName;
    private String entertainmentName;
    private String artistDisplayName;
    private String artistGenre;
    private LocalDate artistDebutDate;
    private String entertainmentCountryCode;
    private String entertainmentHomepageUrl;
    private String promoterName;
    private String youtubeVideoUrl;
}
