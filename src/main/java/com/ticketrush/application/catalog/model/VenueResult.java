package com.ticketrush.application.catalog.model;

import com.ticketrush.domain.venue.Venue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VenueResult {
    private Long id;
    private String name;
    private String city;
    private String countryCode;
    private String address;

    public static VenueResult from(Venue venue) {
        return new VenueResult(
                venue.getId(),
                venue.getName(),
                venue.getCity(),
                venue.getCountryCode(),
                venue.getAddress()
        );
    }
}
