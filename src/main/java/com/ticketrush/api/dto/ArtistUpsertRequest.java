package com.ticketrush.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistUpsertRequest {
    private String name;
    private Long entertainmentId;
    private String displayName;
    private String genre;
    private LocalDate debutDate;
}
