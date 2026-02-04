package com.ticketrush.interfaces.dto;

import com.ticketrush.domain.concert.entity.ConcertOption;
import java.time.LocalDateTime;

public record ConcertOptionResponse(
        Long id,
        LocalDateTime concertDate) {
    public static ConcertOptionResponse from(ConcertOption option) {
        return new ConcertOptionResponse(option.getId(), option.getConcertDate());
    }
}
