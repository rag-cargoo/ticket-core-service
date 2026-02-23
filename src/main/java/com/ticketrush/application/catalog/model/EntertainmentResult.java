package com.ticketrush.application.catalog.model;

import com.ticketrush.domain.entertainment.Entertainment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EntertainmentResult {
    private Long id;
    private String name;
    private String countryCode;
    private String homepageUrl;

    public static EntertainmentResult from(Entertainment entertainment) {
        return new EntertainmentResult(
                entertainment.getId(),
                entertainment.getName(),
                entertainment.getCountryCode(),
                entertainment.getHomepageUrl()
        );
    }
}
