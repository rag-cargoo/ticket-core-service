package com.ticketrush.domain.seed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "seed_markers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seed_markers_marker_key", columnNames = {"marker_key"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeedMarker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "marker_key", nullable = false, length = 120, unique = true)
    private String markerKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SeedMarker(String markerKey, LocalDateTime createdAt) {
        this.markerKey = normalizeRequired(markerKey, "markerKey");
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return trimmed;
    }
}
