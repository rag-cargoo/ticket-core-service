package com.ticketrush.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VenueUpsertRequest {
    private String name;
    private String city;
    private String countryCode;
    private String address;
}
