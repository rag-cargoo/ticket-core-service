package com.ticketrush.api.dto;

import com.ticketrush.domain.venue.Venue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponse {
    private Long id;
    private String name;
    private String city;
    private String countryCode;
    private String address;

    public static VenueResponse from(Venue venue) {
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getCity(),
                venue.getCountryCode(),
                venue.getAddress()
        );
    }
}
