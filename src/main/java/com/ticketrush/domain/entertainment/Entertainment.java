package com.ticketrush.domain.entertainment;

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
@Table(name = "entertainments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entertainment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "HYBE", "SM", "JYP"

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "homepage_url", length = 255)
    private String homepageUrl;

    public Entertainment(String name) {
        this.name = name;
    }

    public Entertainment(String name, String countryCode, String homepageUrl) {
        this.name = name;
        this.countryCode = normalizeCountryCode(countryCode);
        this.homepageUrl = trimToNull(homepageUrl);
    }

    public void rename(String name) {
        String normalized = trimToNull(name);
        if (normalized == null) {
            throw new IllegalArgumentException("name is required");
        }
        this.name = normalized;
    }

    public void updateMetadata(String countryCode, String homepageUrl) {
        if (countryCode != null) {
            this.countryCode = normalizeCountryCode(countryCode);
        }
        if (homepageUrl != null) {
            this.homepageUrl = trimToNull(homepageUrl);
        }
    }

    private String normalizeCountryCode(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
