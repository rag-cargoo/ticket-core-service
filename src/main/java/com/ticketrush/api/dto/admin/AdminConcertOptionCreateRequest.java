package com.ticketrush.api.dto.admin;

import com.ticketrush.api.dto.SeatLayoutRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminConcertOptionCreateRequest {
    private LocalDateTime concertDate;
    private Integer seatCount;
    private SeatLayoutRequest seatLayout;
    private Long ticketPriceAmount;
    private Long venueId;
}
