package com.ticketrush.api.dto;

import com.ticketrush.domain.concert.entity.ConcertOption;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertOptionResponse {
    private Long id;
    private LocalDateTime concertDate;

    public static ConcertOptionResponse from(ConcertOption option) {
        return new ConcertOptionResponse(option.getId(), option.getConcertDate());
    }
}