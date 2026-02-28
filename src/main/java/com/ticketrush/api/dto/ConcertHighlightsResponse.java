package com.ticketrush.api.dto;

import com.ticketrush.application.concert.model.ConcertHighlightsResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertHighlightsResponse {

    private List<ConcertHighlightResponse> openingSoon;
    private List<ConcertHighlightResponse> sellOutRisk;
    private String generatedAt;
    private int openingSoonWithinHours;
    private int sellOutRiskSeatThreshold;
    private int sellOutRiskRatioThreshold;

    public static ConcertHighlightsResponse from(ConcertHighlightsResult result) {
        return new ConcertHighlightsResponse(
                result.getOpeningSoon().stream().map(ConcertHighlightResponse::from).toList(),
                result.getSellOutRisk().stream().map(ConcertHighlightResponse::from).toList(),
                result.getGeneratedAt(),
                result.getOpeningSoonWithinHours(),
                result.getSellOutRiskSeatThreshold(),
                result.getSellOutRiskRatioThreshold()
        );
    }
}
