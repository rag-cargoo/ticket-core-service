package com.ticketrush.domain.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "venues")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "address", length = 255)
    private String address;

    public Venue(String name, String city, String countryCode, String address) {
        this.name = normalizeRequired(name);
        this.city = trimToNull(city);
        this.countryCode = trimToNull(countryCode);
        this.address = trimToNull(address);
    }

    public void update(String name, String city, String countryCode, String address) {
        this.name = normalizeRequired(name);
        this.city = trimToNull(city);
        this.countryCode = trimToNull(countryCode);
        this.address = trimToNull(address);
    }

    private String normalizeRequired(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("name is required");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
