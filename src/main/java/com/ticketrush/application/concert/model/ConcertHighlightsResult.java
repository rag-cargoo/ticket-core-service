package com.ticketrush.application.concert.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConcertHighlightsResult {

    private final List<ConcertHighlightResult> openingSoon;
    private final List<ConcertHighlightResult> sellOutRisk;
    private final String generatedAt;
    private final int openingSoonWithinHours;
    private final int sellOutRiskSeatThreshold;
    private final int sellOutRiskRatioThreshold;
}
