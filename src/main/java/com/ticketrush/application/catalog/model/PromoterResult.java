package com.ticketrush.application.catalog.model;

import com.ticketrush.domain.promoter.Promoter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PromoterResult {
    private Long id;
    private String name;
    private String countryCode;
    private String homepageUrl;

    public static PromoterResult from(Promoter promoter) {
        return new PromoterResult(
                promoter.getId(),
                promoter.getName(),
                promoter.getCountryCode(),
                promoter.getHomepageUrl()
        );
    }
}
