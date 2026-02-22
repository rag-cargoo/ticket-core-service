package com.ticketrush.domain.promoter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Entity
@Getter
@Table(name = "promoters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promoter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "homepage_url", length = 255)
    private String homepageUrl;

    public Promoter(String name, String countryCode, String homepageUrl) {
        this.name = normalizeRequired(name);
        this.countryCode = normalizeCountryCode(countryCode);
        this.homepageUrl = trimToNull(homepageUrl);
    }

    public void update(String name, String countryCode, String homepageUrl) {
        this.name = normalizeRequired(name);
        this.countryCode = normalizeCountryCode(countryCode);
        this.homepageUrl = trimToNull(homepageUrl);
    }

    private String normalizeRequired(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("name is required");
        }
        return normalized;
    }

    private String normalizeCountryCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
