package com.ticketrush.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AgencyUpsertRequest {
    private String name;
    private String countryCode;
    private String homepageUrl;
}
