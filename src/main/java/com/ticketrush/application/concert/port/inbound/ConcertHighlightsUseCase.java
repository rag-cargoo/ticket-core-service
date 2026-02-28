package com.ticketrush.application.concert.port.inbound;

import com.ticketrush.application.concert.model.ConcertHighlightsResult;

import java.time.Instant;

public interface ConcertHighlightsUseCase {

    ConcertHighlightsResult getHighlights(Integer openingSoonLimit, Integer sellOutRiskLimit, Instant serverNow);
}
