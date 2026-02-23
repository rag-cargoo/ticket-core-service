package com.ticketrush.api.dto;

import com.ticketrush.application.concert.model.ConcertOptionResult;
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
    private Long ticketPriceAmount;
    private Long venueId;
    private String venueName;
    private String venueCity;
    private String venueCountryCode;
    private String venueAddress;

    public static ConcertOptionResponse from(ConcertOptionResult option) {
        return new ConcertOptionResponse(
                option.getId(),
                option.getConcertDate(),
                option.getTicketPriceAmount(),
                option.getVenueId(),
                option.getVenueName(),
                option.getVenueCity(),
                option.getVenueCountryCode(),
                option.getVenueAddress()
        );
    }
}
