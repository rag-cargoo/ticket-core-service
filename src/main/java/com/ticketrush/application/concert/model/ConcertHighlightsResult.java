package com.ticketrush.application.concert.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertHighlightsResult {
    private List<ConcertHighlightItemResult> openingSoon;
    private List<ConcertHighlightItemResult> sellOutRisk;
    private LocalDateTime generatedAt;
    private int openingSoonWithinHours;
    private int sellOutRiskSeatThreshold;
    private double sellOutRiskRatioThreshold;
}
