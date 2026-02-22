package com.ticketrush.api.dto;

import com.ticketrush.domain.entertainment.Entertainment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EntertainmentResponse {
    private Long id;
    private String name;
    private String countryCode;
    private String homepageUrl;

    public static EntertainmentResponse from(Entertainment entertainment) {
        return new EntertainmentResponse(
                entertainment.getId(),
                entertainment.getName(),
                entertainment.getCountryCode(),
                entertainment.getHomepageUrl()
        );
    }
}
