package com.ticketrush.api.dto;

import com.ticketrush.domain.agency.Agency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AgencyResponse {
    private Long id;
    private String name;
    private String countryCode;
    private String homepageUrl;

    public static AgencyResponse from(Agency agency) {
        return new AgencyResponse(
                agency.getId(),
                agency.getName(),
                agency.getCountryCode(),
                agency.getHomepageUrl()
        );
    }
}
