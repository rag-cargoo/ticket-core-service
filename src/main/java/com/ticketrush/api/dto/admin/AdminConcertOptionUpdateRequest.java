package com.ticketrush.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminConcertOptionUpdateRequest {
    private LocalDateTime concertDate;
    private Long ticketPriceAmount;
    private Long venueId;
}
