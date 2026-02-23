package com.ticketrush.application.concert.model;

import com.ticketrush.domain.concert.entity.ConcertOption;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertOptionResult {
    private Long id;
    private LocalDateTime concertDate;
    private Long ticketPriceAmount;
    private Long venueId;
    private String venueName;
    private String venueCity;
    private String venueCountryCode;
    private String venueAddress;

    public static ConcertOptionResult from(ConcertOption option) {
        var venue = option.getVenue();
        return new ConcertOptionResult(
                option.getId(),
                option.getConcertDate(),
                option.getTicketPriceAmount(),
                venue != null ? venue.getId() : null,
                venue != null ? venue.getName() : null,
                venue != null ? venue.getCity() : null,
                venue != null ? venue.getCountryCode() : null,
                venue != null ? venue.getAddress() : null
        );
    }
}
