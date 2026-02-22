package com.ticketrush.api.dto;

import com.ticketrush.domain.promoter.Promoter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PromoterResponse {
    private Long id;
    private String name;
    private String countryCode;
    private String homepageUrl;

    public static PromoterResponse from(Promoter promoter) {
        return new PromoterResponse(
                promoter.getId(),
                promoter.getName(),
                promoter.getCountryCode(),
                promoter.getHomepageUrl()
        );
    }
}
